package anders.akita.test;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TestAll {

	final static private Logger logger = Logger.getLogger(TestAll.class);
	
	public static void main(String[] args) {

		PropertyConfigurator.configure("conf/log4j.properties");
		logger.info("SQLParserTest: " + SQLParserTest.test());
		
	}

}
