package anders.akita.test;

import java.io.*;

import org.apache.log4j.*;

import anders.akita.parser.*;
import anders.util.Util;

public class SQLUdfTest {
	
	final static private Logger logger = Logger.getLogger(SQLUdfTest.class);
	
	public static boolean test()
	{
		
		Util.initLog4j();
		/*
		try{
		ZqlJJParser parser = new ZqlJJParser(new FileInputStream("indata/udftest.txt"));
			while(true){
				ZUdf udf = parser.SQLUdf();
				if(udf == null)	
					return true;
				
				StringBuilder log = new StringBuilder(udf.getName());
				log.append("\n");
				for(int i = 0; i < udf.getParmN(); ++i){
					if(i > 0)log.append(",");
					log.append(udf.getParm(i)).append(":").append(udf.getParmAggr(i));
				}
				log.append("\n").append(udf.getExp()).append("\n");
				logger.info(log);
			}
		}
		catch(Exception e){
			logger.error(e.toString() + ":" + e.getMessage());
			return false;
		}
		*/
		try{Class.forName("anders.akita.parser.FunctionMgr");}
		catch(Exception e){return false;}
		return true;
		
	}
	public static void main(String[] args){
		Util.initLog4j();
		logger.info("SQL UDF test: " + test());
	}

}
