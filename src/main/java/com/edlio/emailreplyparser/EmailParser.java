package com.edlio.emailreplyparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;



public class EmailParser {
	static final String SIG_REGEX = "(^--|^__|\\w-$)|(^(\\w+\\s*){1,3} ym morf tneS$)";
	static final String QUOTE_REGEX = "(>+)$";
	private static List<Pattern> compiledQuoteHeaderPatterns;
	
	private List<String> quoteHeadersRegex = new ArrayList<String>();
	private List<FragmentDTO> fragments = new ArrayList<FragmentDTO>();
	private Collection<String> timedoutRegexes = new LinkedList<String>();
	private ExecutorService regexCheckService = null;
	private long timeout = 5000L;
	
	public EmailParser() {
		compiledQuoteHeaderPatterns = new ArrayList<Pattern>();
		quoteHeadersRegex.add("^(On\\s(.{1,500})wrote:)");
		quoteHeadersRegex.add("From:[^\\n]+\\n?([^\\n]+\\n?){0,2}To:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		quoteHeadersRegex.add("To:[^\\n]+\\n?([^\\n]+\\n?){0,2}From:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		quoteHeadersRegex.add("Date:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		
	}
	
	public void compileQuoteHeaderRegexes() {
		for (String regex : quoteHeadersRegex) {
			compiledQuoteHeaderPatterns.add(Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL));
		}
		
	}
	private class regexCheck implements Callable <Boolean> {
		Pattern p;
		String content;
		public regexCheck(Pattern p, String content) {
			this.p = p;
			this.content = content;
		}

		public Boolean call() throws Exception {
			if (p.matcher(content).find()) {
				return true;
			}
			return false;
		}
	
	}
	
	public String read () {
		return QUOTE_REGEX;
	}
	
	public Email parse(String emailText) {
		compileQuoteHeaderRegexes();
		emailText.replace("\r\n", "\n");
		
		FragmentDTO fragment = null;
		
		String[] lines = new StringBuilder(emailText).reverse().toString().split("\n");
		List<String> paragraph = new ArrayList<String>();
		
		for (String line : lines) {
			line = StringUtils.stripEnd(line, "\n");
			if (!isSignature(line))
				line = StringUtils.stripStart(line, null);
			
			if (fragment != null && line.isEmpty()) {
				String last = fragment.lines.get(fragment.lines.size()-1);
				
				if (isSignature(last)) {
					fragment.isSignature = true;
					addFragment(fragment);
					
					fragment = null;
				} 
				else if (isMultiLineQuoteHeaders(paragraph)) {
					fragment.isQuoted = true;
					addFragment(fragment);
					
					fragment = null;
				}
				else if (isQuoteHeader(last)) {
					fragment.isQuoted = true;
					addFragment(fragment);
					
					fragment = null;
				}
				paragraph.clear();
			}
			
			boolean isQuoted = isQuote(line);
			
			if (fragment == null || !isFragmentLine(fragment, line, isQuoted)) {
				if (fragment != null)
					addFragment(fragment);
				
				fragment = new FragmentDTO();
				fragment.isQuoted = isQuoted;
				fragment.lines = new ArrayList<String>();
			}
			fragment.lines.add(line);	
			if (!line.isEmpty()) {
				paragraph.add(line);
			}
		}
		
		if (fragment != null)
			addFragment(fragment);
		
		return createEmail(fragments);
	}
	
	public List<String> getQuoteHeadersRegex() {
		return this.quoteHeadersRegex;
	}
	
	public void setQuoteHeadersRegex(List<String> newRegex) {
		this.quoteHeadersRegex = newRegex;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public long getTimeout() {
		return this.timeout;
	}
	
	public Collection<String> getTimedoutRegexes() {
		return timedoutRegexes;
	}
	
	protected Email createEmail(List<FragmentDTO> fragmentDTOs) {
		List <Fragment> fs = new ArrayList<Fragment>();
		Collections.reverse(fragmentDTOs);
		for (FragmentDTO f : fragmentDTOs) {
			
			String content = new StringBuilder(StringUtils.join(f.lines,"\n")).reverse().toString().replaceAll("/^\n/", "");
			Fragment fr = new Fragment(content, f.isHidden, f.isSignature, f.isQuoted);
			fs.add(fr);
		}
		return new Email(fs);
	}
		
	private boolean isQuoteHeader(String line) {
		regexCheckService = Executors.newCachedThreadPool();
		for(Pattern p : compiledQuoteHeaderPatterns) {
			Boolean matches;
			Future<Boolean> checkResult = null;
			checkResult = regexCheckService.submit(new regexCheck(p, new StringBuilder(line).reverse().toString()));
			
			try {
				matches = checkResult.get(timeout, TimeUnit.MILLISECONDS);
				
			} catch (TimeoutException ex) {
				checkResult.cancel(true);
				return false;
			} catch (InterruptedException e) {
				checkResult.cancel(true);
				return false;
			} catch (ExecutionException e) {
				checkResult.cancel(true);
				return false;
			} 
			if (matches) {
				return matches;
			}
			
		}
		return false;
	}
	
	private boolean isSignature(String line) {
		Pattern p = Pattern.compile(SIG_REGEX, Pattern.DOTALL);
		Matcher m = p.matcher(line);
		boolean find = m.find();
		return find;
	}
	
	private boolean isQuote(String line) {
		Pattern p = Pattern.compile(QUOTE_REGEX, Pattern.DOTALL);
		Matcher m = p.matcher(line);
		return m.find();
	}
	
	private boolean isEmpty(FragmentDTO fragment) {
		return "".equals(StringUtils.join(fragment.lines,""));
	}
	
	private boolean isFragmentLine(FragmentDTO fragment, String line, boolean isQuoted) {
		return fragment.isQuoted == isQuoted || (fragment.isQuoted && (isQuoteHeader(line) || line.isEmpty()));
	}
	
	private void addFragment(FragmentDTO fragment) {
		if (fragment.isQuoted || fragment.isSignature || isEmpty(fragment)) 
			fragment.isHidden = true;
		
		fragments.add(fragment);
	}
	
	private boolean isMultiLineQuoteHeaders(List<String> paragraph) {
		if (paragraph.size() > 6)
			return false;
		
		String content = new StringBuilder(StringUtils.join(paragraph,"\n")).reverse().toString();
		regexCheckService = Executors.newCachedThreadPool();
		for(Pattern p : compiledQuoteHeaderPatterns) {
			Boolean matches;
			Future<Boolean> checkResult = null;
			checkResult = regexCheckService.submit(new regexCheck(p, content));
			
			try {
				matches = checkResult.get(timeout, TimeUnit.MILLISECONDS);
				
			} catch (TimeoutException ex) {
				checkResult.cancel(true);
				return false;
			} catch (InterruptedException e) {
				checkResult.cancel(true);
				return false;
			} catch (ExecutionException e) {
				checkResult.cancel(true);
				return false;
			} 
			if (matches) {
				return matches;
			}
			
		}
		
		return false;

	}	
}