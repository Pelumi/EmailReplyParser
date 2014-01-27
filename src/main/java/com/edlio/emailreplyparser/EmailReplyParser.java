package com.edlio.emailreplyparser;

public class EmailReplyParser {

	public static Email read(String emailText) {
		if (emailText == null)
			emailText = "";

		EmailParser parser = new EmailParser();
		return parser.parse(emailText);
	}
	
	public static String parseReply(String emailText) {
		return read(emailText).getVisibleText();
	}

}
