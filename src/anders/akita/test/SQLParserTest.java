package anders.akita.test;

import org.apache.log4j.*;
import anders.akita.parser.*;
import anders.util.Util;

import java.io.*;
import java.util.*;



public class SQLParserTest {

	final static private Logger logger = Logger.getLogger(SQLParserTest.class);
	@SuppressWarnings("deprecation")
	public static boolean test() 
	{
		
		boolean result = true;
		
		try{
			
			Scanner sc = new Scanner(new FileInputStream("indata/sqltest.txt"));
			PrintStream os = new PrintStream("outdata/sqltest.txt");
			
			
			while (sc.hasNextLine()) {
				ZqlJJParser parser = null;
				try {
					String line = sc.nextLine().trim();

					if(line.equals("") || line.startsWith("//") )
						continue;
					
					os.println(line);
					
					parser = new ZqlJJParser(new StringBufferInputStream(line));
					ZStatement zq = parser.SQLStatement();
					os.println(zq.toString());
					
					os.println();
				} catch (ParseException pe) {
					os.println(pe.getMessage());
					parser.ReInit(System.in);
					
					result = false;
				} 
				catch (anders.akita.parser.TokenMgrError te) {
					os.println(te.getMessage());
					parser.ReInit(System.in);
	
					result = false;
				} 
			}
			sc.close();
			os.close();
			
			//negative test
			sc = new Scanner(new FileInputStream("indata/sqltest-neg.txt"));
			os = new PrintStream("outdata/sqltest-neg.txt");
			
			
			while (sc.hasNextLine()) {
				ZqlJJParser parser = null;
				try {
					String line = sc.nextLine().trim();

					if(line.equals("") || line.startsWith("//") )
						continue;
					
					os.println(line);
					
					parser = new ZqlJJParser(new StringBufferInputStream(line));
					ZStatement zq = parser.SQLStatement();

					os.println(zq.toString());
					
					os.println("TEST FAIL !!!!");
					result = false;
					
				} catch (ParseException pe) {
					os.println(pe.getMessage());
					parser.ReInit(System.in);
					os.println();
				} 
				catch (anders.akita.parser.TokenMgrError te) {
					os.println(te.getMessage());
					parser.ReInit(System.in);
					
					result = false;
				} 
			}
			sc.close();
			os.close();
			
		}
		catch(Exception e){
			logger.error("error: " + Util.exceptionStackTrace(e));
			result = false;
		}
		return result;
	}
	
	public static void main(String[] args)
		throws IOException
	{
		Util.initLog4j();
		logger.info( "result: " + test());
	}

}
