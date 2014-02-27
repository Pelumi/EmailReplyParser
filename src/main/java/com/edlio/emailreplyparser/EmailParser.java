package com.edlio.emailreplyparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;



public class EmailParser {
	
	static final Pattern SIG_PATTERN = Pattern.compile( "((^Sent from my (\\s*\\w+){1,3}$)|(^-\\w|__|--|\u2013|\u2014))", Pattern.DOTALL);
	static final Pattern QUOTE_PATTERN = Pattern.compile("(^>+)", Pattern.DOTALL);
	private static List<Pattern> compiledQuoteHeaderPatterns;
	
	private List<String> quoteHeadersRegex = new ArrayList<String>();
	private List<FragmentDTO> fragments = new ArrayList<FragmentDTO>();
	private int maxParagraphLines;
	private int maxNumCharsEachLine;
	
	
	/**
	 * Initialize EmailParser.
	 */
	public EmailParser() {
		compiledQuoteHeaderPatterns = new ArrayList<Pattern>();
		quoteHeadersRegex.add("^(On\\s(.{1,500})wrote:)");
		quoteHeadersRegex.add("From:[^\\n]+\\n?([^\\n]+\\n?){0,2}To:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		quoteHeadersRegex.add("To:[^\\n]+\\n?([^\\n]+\\n?){0,2}From:[^\\n]+\\n?([^\\n]+\\n?){0,2}Subject:[^\\n]+");
		maxParagraphLines = 6;
		maxNumCharsEachLine = 200;
	}

	/**
	 * Splits the given email text into a list of {@link Fragment} and returns the {@link Email} object. 
	 * 
	 * @param emailText
	 * @return
	 */
	public Email parse(String emailText) {
		emailText = emailText.replaceAll("\u2014", "------");
		compileQuoteHeaderRegexes();
		
		// Normalize line endings
		emailText.replace("\r\n", "\n");
		
		FragmentDTO fragment = null;
		
		// Split body to multiple lines.
		String[] lines = new StringBuilder(emailText).toString().split("\n");
		/* Reverse the array.
		 * 
		 * Reversing the array makes us to parse from the bottom to the top.  
		 * This way we can check for quote headers lines above quoted blocks
		 */
		ArrayUtils.reverse(lines);
		
		/* Paragraph for multi-line quote headers.
		 * Some clients break up the quote headers into multiple lines.
	         */
		List<String> paragraph = new ArrayList<String>();
		
		// Scans the given email line by line and figures out which fragment it belong to.
		for (String line : lines) {
			// Strip new line at the end of the string 
			line = StringUtils.stripEnd(line, "\n");
			// Strip empty spaces at the end of the string
			line = StringUtils.stripEnd(line, null);
			
			/* If the fragment is not null and we hit the empty line,
			 * we get the last line from the fragment and check if the last line is either
			 * signature and quote headers.
			 * If it is, add fragment to the list of fragments and delete the current fragment.
			 * Also, delete the paragraph.
			 */
			if (fragment != null && line.isEmpty()) {
				String last = fragment.lines.get(fragment.lines.size()-1);
				
				if (isSignature(last)) {
					fragment.isSignature = true;
					addFragment(fragment);
					
					fragment = null;
				} 
				else if (isQuoteHeader(paragraph)) {
					fragment.isQuoted = true;
					addFragment(fragment);
					
					fragment = null;
				}
				paragraph.clear();
			}
			
			// Check if the line is a quoted line.
			boolean isQuoted = isQuote(line);
			
			/*
			 * If fragment is empty or if the line does not matches the current fragment,
			 * create new fragment.
			 */
			if (fragment == null || !isFragmentLine(fragment, line, isQuoted)) {
				if (fragment != null)
					addFragment(fragment);
				
				fragment = new FragmentDTO();
				fragment.isQuoted = isQuoted;
				fragment.lines = new ArrayList<String>();
			}
			
			// Add line to fragment and paragraph
			fragment.lines.add(line);	
			if (!line.isEmpty()) {
				paragraph.add(line);
			}
		}
		
		if (fragment != null)
			addFragment(fragment);
		
		return createEmail(fragments);
	}
	
	/**
	 * Returns existing quote headers regular expressions.
	 * 
	 * @return
	 */
	public List<String> getQuoteHeadersRegex() {
		return this.quoteHeadersRegex;
	}
	
	/**
	 * Sets quote headers regular expressions.
	 * 
	 * @param newRegex
	 */
	public void setQuoteHeadersRegex(List<String> newRegex) {
		this.quoteHeadersRegex = newRegex;
	}
	
	/**
	 * Gets max number of lines allowed for each paragraph when checking quote headers.
	 * @return
	 */
	public int getMaxParagraphLines() {
		return this.maxParagraphLines;
	}
	
	/**
	 * Sets max number of lines allowed for each paragraph when checking quote headers.
	 * 
	 * @param maxParagraphLines
	 */
	public void setMaxParagraphLines(int maxParagraphLines) {
		this.maxParagraphLines = maxParagraphLines;
	}
	
	/**
	 * Gets max number of characters allowed for each line when checking quote headers.
	 * 
	 * @return
	 */
	public int getMaxNumCharsEachLine() {
		return maxNumCharsEachLine;
	}
	
	/**
	 * Sets max number of characters allowed for each line when checking quote headers.
	 * @param maxNumCharsEachLine
	 */
	public void setMaxNumCharsEachLine(int maxNumCharsEachLine) {
		this.maxNumCharsEachLine = maxNumCharsEachLine;
	}
	
	/**
	 * Creates {@link Email} object from List of fragments.
	 * @param fragmentDTOs
	 * @return
	 */
	protected Email createEmail(List<FragmentDTO> fragmentDTOs) {
		List <Fragment> fs = new ArrayList<Fragment>();
		Collections.reverse(fragmentDTOs);
		for (FragmentDTO f : fragmentDTOs) {
			Collections.reverse(f.lines);
			String content = new StringBuilder(StringUtils.join(f.lines,"\n")).toString();
			Fragment fr = new Fragment(content, f.isHidden, f.isSignature, f.isQuoted);
			fs.add(fr);
		}
		return new Email(fs);
	}
	
	/**
	 * Compile all the quote headers regular expressions before the parsing.
	 * 
	 */
	private void compileQuoteHeaderRegexes() {
		for (String regex : quoteHeadersRegex) {
			compiledQuoteHeaderPatterns.add(Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL));
		}
	}
	
	/**
	 * Check if the line is a signature.
	 * @param line
	 * @return
	 */
	private boolean isSignature(String line) {
		boolean find = SIG_PATTERN.matcher(line).find();
		return find;
	}
	
	/**
	 * Checks if the line is quoted line.
	 * @param line
	 * @return
	 */
	private boolean isQuote(String line) {
		return QUOTE_PATTERN.matcher(line).find();
	}
	
	/**
	 * Checks if lines in the fragment are empty. 
	 * @param fragment
	 * @return
	 */
	private boolean isEmpty(FragmentDTO fragment) {
		return StringUtils.join(fragment.lines,"").isEmpty();
	}
	
	/**
	 * If the line matches the current fragment, return true.  
	 * Note that a common reply header also counts as part of the quoted Fragment, 
	 * even though it doesn't start with `>`.
	 * 
	 * @param fragment
	 * @param line
	 * @param isQuoted
	 * @return
	 */
	private boolean isFragmentLine(FragmentDTO fragment, String line, boolean isQuoted) {
		return fragment.isQuoted == isQuoted || (fragment.isQuoted && (isQuoteHeader(Arrays.asList(line)) || line.isEmpty()));
	}
	
	/**
	 * Add fragment to fragments list.
	 * @param fragment
	 */
	private void addFragment(FragmentDTO fragment) {
		if (fragment.isQuoted || fragment.isSignature || isEmpty(fragment)) 
			fragment.isHidden = true;
		
		fragments.add(fragment);
	}
	
	/**
	 * Checks if the given multiple-lines paragraph has one of the quote headers.
	 * Returns false if it doesn't contain any of the quote headers, 
	 * if paragraph lines are greater than maxParagraphLines, or line has more than maxNumberCharsEachLine characters.
	 *   
	 * @param paragraph
	 * @return
	 */
	private boolean isQuoteHeader(List<String> paragraph) {
		if (paragraph.size() > maxParagraphLines)
			return false;
		for (String line : paragraph) {
			if (line.length() > maxNumCharsEachLine)
				return false;
		}
		Collections.reverse(paragraph);
		String content = new StringBuilder(StringUtils.join(paragraph,"\n")).toString();
		for(Pattern p : compiledQuoteHeaderPatterns) {
			if (p.matcher(content).find()) {
				return true;
			}
		}
		
		return false;

	}	
}
