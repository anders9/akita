package anders.akita.test;

import org.apache.log4j.*;

import anders.util.Util;

public class TestAll {

	final static private Logger logger = Logger.getLogger(TestAll.class);
	
	public static void main(String[] args) {

		Util.initLog4j();
		System.out.println("SQLParserTest: " + SQLParserTest.test());
		System.out.println("SQLUDFtest: " + SQLUdfTest.test());
	}

}
