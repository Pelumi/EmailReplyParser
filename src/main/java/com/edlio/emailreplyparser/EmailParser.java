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
	
	private List<String> quoteHeadersRegex = new ArrayList<String>();
	private List<FragmentDTO> fragments = new ArrayList<FragmentDTO>();
	private Collection<String> timedoutRegexes = new LinkedList<String>();
	private ExecutorService regexCheckService = null;
	private long timeout = 10000L;
	
	public EmailParser() {
		quoteHeadersRegex.add("^(On\\s(.+)wrote:)");
		quoteHeadersRegex.add("From:[^\\n]+\\n?([^\\n]+\\n?){0,2}To:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		quoteHeadersRegex.add("To:[^\\n]+\\n?([^\\n]+\\n?){0,2}From:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		quoteHeadersRegex.add("Date:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
	}
	
	private class regexCheck implements Callable <List<String>> {
		Pattern p;
		String emailText;
		public regexCheck(String regex, String emailText) {
			p =Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
			this.emailText = emailText;
		}

		public List<String> call() throws Exception {
			Matcher m = p.matcher(emailText);
			List<String> matches = new ArrayList<String>();
			while (m.find()){
			    matches.add(m.group());
			}
			return matches;
		}
	
	}
	
	public String read () {
		return QUOTE_REGEX;
	}
	
	public Email parse(String emailText) {
		emailText.replace("\r\n", "\n");
		regexCheckService = Executors.newCachedThreadPool();
		for(String regex : quoteHeadersRegex) {
			List<String> matches = new ArrayList<String>();
			Future<List<String>> checkResult = null;
			checkResult = regexCheckService.submit(new regexCheck(regex, emailText));
			
			try {
				matches = checkResult.get(timeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException ex) {
				checkResult.cancel(true);
				System.out.println("timeout");
				timedoutRegexes.add(regex);
			} catch (InterruptedException e) {
				checkResult.cancel(true);
				timedoutRegexes.add(regex);
			} catch (ExecutionException e) {
				checkResult.cancel(true);
				timedoutRegexes.add(regex);
			} finally {
				if(checkResult.isDone() && !checkResult.isCancelled()) {
					if (!matches.isEmpty()) {
						String match = matches.get(0);
						emailText = emailText.replace(matches.get(0), match.replace("\n", ""));
					}
				}
			}
			
		}
		
		FragmentDTO fragment = null;
		
		String[] lines = new StringBuilder(emailText).reverse().toString().split("\n");
		
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
				else if (isQuoteHeader(last)) {
					fragment.isQuoted = true;
					addFragment(fragment);
					
					fragment = null;
				}
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
		for (String qhregex : quoteHeadersRegex) {
			Pattern p = Pattern.compile(qhregex, Pattern.MULTILINE | Pattern.DOTALL);
			Matcher m = p.matcher(new StringBuilder(line).reverse().toString());
			if (m.find())
				return true;
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
	
	
}