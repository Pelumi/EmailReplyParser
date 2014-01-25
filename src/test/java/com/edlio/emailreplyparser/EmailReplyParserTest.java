package com.edlio.emailreplyparser;

import static org.junit.Assert.*;

import org.junit.Test;

public class EmailReplyParserTest {

	@Test
	public void testReadWithNullContent() {
		Email email = EmailReplyParser.read(null);
		assertEquals("", email.getVisibleText());
	}
	
	@Test
	public void testReadWithEmptyContent() {
		Email email = EmailReplyParser.read("");
		assertEquals("", email.getVisibleText());
		
	}

}
