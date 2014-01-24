package com.edlio.emailreplyparser;

public class Temporary {
	public static void main(String [] args)
	{
		String emailText = "Hello";
		String parsedEmail = EmailReplyParser.parseReply(emailText);
		System.out.println(parsedEmail);
	}

}
