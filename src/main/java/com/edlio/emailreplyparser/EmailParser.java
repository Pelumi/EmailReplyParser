package com.edlio.emailreplyparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public class EmailParser {
	static final String SIG_REGEX = "/(^--|^__|\\w-$)|(^(\\w+\\s*){1,3} ym morf tneS$)/s";
	static final String QUOTE_REGEX = "/(>+)$/s";
	
	private List<String> quoteHeadersRegex = new ArrayList<String>();
	private List<FragmentDTO> fragments = new ArrayList<FragmentDTO>(); 
	
	public EmailParser() {
		quoteHeadersRegex.add("/^(On\\s(.+)wrote:)$/ms");
		
	}
	
	public String read () {
		return QUOTE_REGEX;
	}
	
	public Email parse(String emailText) {
		emailText.replace("\r\n", "\n");
		for(String regex : quoteHeadersRegex) {
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(emailText);
			List<String> matches = new ArrayList<String>();
			while(m.find()){
			    matches.add(m.group());
			}
			if(!matches.isEmpty()) {
				String match = matches.get(0);
				emailText = emailText.replace(matches.get(0), match.replace("\n", ""));
			}
			
		}
		FragmentDTO fragment = null;
		for(String line : new StringBuilder(emailText).reverse().toString().split("\n")) {
			StringUtils.stripEnd(line, "\n");
			
			if(!isSignature(line))
				StringUtils.stripStart(line, null);
			
			if(fragment != null && line.isEmpty()) {
				String last = fragment.lines.get(fragment.lines.size()-1);
				
				if(isSignature(last)) {
					fragment.isSignature = true;
					addFragment(fragment);
					
					fragment = null;
				} 
				else if (isQuoteHeader(line)) {
					fragment.isQuoted = true;
					addFragment(fragment);
					
					fragment = null;
				}
			}
			
			boolean isQuoted = isQuote(line);
			
			if(fragment == null || !isFragmentLine(fragment, line, isQuoted)) {
				if(fragment != null)
					addFragment(fragment);
				
				fragment = new FragmentDTO();
				fragment.isQuoted = isQuoted;
				fragment.lines = new ArrayList<String>();
			}
			fragment.lines.add(line);	
		}
		if(fragment != null)
			addFragment(fragment);
		return createEmail(fragments);
	}
	
	public List<String> getQuoteHeadersRegex() {
		return this.quoteHeadersRegex;
		
	}
	
	public void setQuoteHeadersRegex(List<String> newRegex) {
		this.quoteHeadersRegex = newRegex;
			
	}
	
	protected Email createEmail(List<FragmentDTO> fragmentDTOs) {
		List <Fragment> fs = new ArrayList<Fragment>();
		Collections.reverse(fragmentDTOs);
		for(FragmentDTO f : fragmentDTOs) {
			
			fs.add(new Fragment(new StringBuilder(StringUtils.join(f.lines,"\n")).reverse().toString().replaceAll("/^\n/", ""), f.isHidden, f.isSignature, f.isQuoted));
		}
		return new Email(fs);
	}
		
	private boolean isQuoteHeader(String line) {
		for(String qhregex : quoteHeadersRegex) {
			if(line.matches(qhregex))
				return true;
		}
		return false;
		
	}
	
	private boolean isSignature(String line) {
		return line.matches(SIG_REGEX);
	}
	
	private boolean isQuote(String line) {
		return line.matches(QUOTE_REGEX);
	}
	
	private boolean isEmpty(FragmentDTO fragment) {
		return "".equals(StringUtils.join(fragment.lines,""));
	}
	
	private boolean isFragmentLine(FragmentDTO fragment, String line, boolean isQuoted) {
		return fragment.isQuoted == isQuoted || (fragment.isQuoted && (isQuoteHeader(line) || line.isEmpty()));
	}
	
	private void addFragment(FragmentDTO fragment) {
		if(fragment.isQuoted || fragment.isSignature || isEmpty(fragment)) 
			fragment.isHidden = true;
		
		fragments.add(fragment);

	}
	
}