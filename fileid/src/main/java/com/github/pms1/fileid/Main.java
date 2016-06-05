package com.github.pms1.fileid;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) {
		for (String a : args) {
			try {
				System.out.println(a + ": " + FileKeyGen.getFileKeyBasicAttributes(Paths.get(a)).fileKey());
			} catch (IOException e) {
				System.out.println(a + ": " + e);
			}
		}
	}
}
