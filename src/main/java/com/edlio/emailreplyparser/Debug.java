package com.edlio.emailreplyparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Debug {
	public static void main(String [] args) {
		BufferedReader br = null;

		String emailText = "";
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader("Fixtures/email_custom_quote_header.txt"));
 
			while ((sCurrentLine = br.readLine()) != null) {
				emailText += sCurrentLine + "\n";
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		EmailParser parser = new EmailParser();
		List<String> regex = parser.getQuoteHeadersRegex();
		regex.add("^(\\d{4}(.+)rta:)");
		parser.setQuoteHeadersRegex(regex);
		
		Email email = parser.parse(emailText);
		String parsedEmail = email.getVisibleText();
		System.out.println("================================Original Email Text================================");
		System.out.println(emailText);
		System.out.println("===================================================================================\n");
		System.out.println("================================Parsed  Email  Text================================");
		System.out.println(parsedEmail);
		System.out.println("===================================================================================\n");
	}

}