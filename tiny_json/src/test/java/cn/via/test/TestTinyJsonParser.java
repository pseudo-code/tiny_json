package cn.via.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import cn.via.TinyJsonParser;

public class TestTinyJsonParser {

	@Test
	public void test() {
		Object parse = TinyJsonParser.parse(TestTokenizer.TEST_STR);
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testokstr1() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonokstr1.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testokstr2() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonokstr2.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testerrorstr1() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonerrorstr1.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testerrorstr2() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonerrorstr2.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testerrorstr3() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonerrorstr3.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testerrorstr4() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonerrorstr4.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	@Test
	public void testerrorstr5() throws IOException {
		Object parse = TinyJsonParser.parse(readFile("jsonerrorstr5.txt"));
		System.out.println(TinyJsonParser.getPrintString(parse));
	}
	
	
	private static final String readFile(String fileName) throws IOException {
		
		InputStream in = TestTinyJsonParser.class.getClassLoader()
				.getResourceAsStream(fileName);
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		char[] c = new char[1024];
		int count = -1;
		while((count = reader.read(c, 0, c.length)) != -1) {
			sb.append(c, 0, count);
		}
		return sb.toString();
	}
	
}
