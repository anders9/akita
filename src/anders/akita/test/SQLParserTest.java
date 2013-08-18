package anders.akita.test;


import anders.akita.parser.*;
import java.io.*;

public class SQLParserTest {

	public static void main(String[] args)
		throws IOException
	{

		InputStream is = new FileInputStream("indata/sqltest.txt");
		PrintStream os = new PrintStream("outdata/sqltest.txt");
		
		ZqlJJParser parser = new ZqlJJParser(is);
		while (true) {
			try {
				//os.println("Input SQL below:");
				ZStatement zq = parser.SQLStatement();
				if(zq == null)
					break;
				os.println(zq.toString());
			} catch (ParseException pe) {
				os.println(pe.getMessage());
				parser.ReInit(System.in);
			} 
			catch (anders.akita.parser.TokenMgrError te) {
				os.println(te.getMessage());
				parser.ReInit(System.in);
			} 
		}
		is.close();
		os.close();
	}

}
