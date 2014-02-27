package com.edlio.emailreplyparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FixtureGetter {

	static String getFixture(String fileName) {
		BufferedReader br = null;

		String emailText = "";
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new InputStreamReader (new FileInputStream("src/test/fixtures/" + fileName), "UTF-8"));
 
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
