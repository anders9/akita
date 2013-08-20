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
					
					os.println("READ: " + line);
					
					parser = new ZqlJJParser(new StringBufferInputStream(line));
					ZStatement zq = parser.SQLStatement();
					if(zq == null)
						break;
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
		}
		catch(Exception e){
			logger.error("error: " + e.getMessage());
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
