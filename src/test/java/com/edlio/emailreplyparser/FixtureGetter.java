package com.edlio.emailreplyparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FixtureGetter {

	static String getFixture(String fileName) {
		BufferedReader br = null;

		String emailText = "";
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader("src/test/fixtures/" + fileName));
 
			while ((sCurrentLine = br.readLine()) != null) {
				emailText += sCurrentLine + "\n";
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				if (br != null)
					br.close();
			} 
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return emailText;
	}
}
