package anders.akita.test;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import anders.akita.exec.Executor;
import anders.akita.meta.*;
import anders.akita.parser.*;

public class ExecTest {

	//final static private Logger logger = Logger.getLogger(ExecTest.class);
	
	interface Handler{
		public void exec(ZStatement stmt);
	}
	private static void readStmt(InputStream in, Handler handler)
		throws IOException
	{	
			Scanner sc = new Scanner(in);
			//PrintStream os = new PrintStream(System.out);
			
			while (sc.hasNextLine()) {
				ZqlJJParser parser = null;
				try {
					
					String line = sc.nextLine().trim();

					if(line.equals("") || line.startsWith("//") )
						continue;
					
					System.out.println(line);
					
					parser = new ZqlJJParser(new StringBufferInputStream(line));
					ZStatement stmt = parser.SQLStatement();
					System.out.println(stmt.toString());
					handler.exec(stmt);
					
					System.out.println();
				} catch (ParseException pe) {
					System.out.println(pe.getMessage());
					//parser.ReInit(System.in);
				} 
				catch (anders.akita.parser.TokenMgrError te) {
					System.out.println(te.getMessage());
					//parser.ReInit(System.in);
				} 
				System.out.print("SQL << ");
			}
			sc.close();
	}
	
	public static void main(String[] args) {
		try{
			FunctionMgr.init();
			
			//System.out.print("SQL << ");
			readStmt(new FileInputStream("indata/schema.txt"), new Handler(){
				public void exec(ZStatement stmt) {
					if(stmt instanceof ZCreateTable){
						Meta.createTable((ZCreateTable)stmt);
					}
				}
			});
			readStmt(System.in, new Handler(){
				public void exec(ZStatement stmt) {
					if(stmt instanceof ZQuery){
						try{
							Executor.INSTANCE.exec((ZQuery)stmt, System.out);
						}catch(ExecException e){
							System.out.println(e.toString());
						}
					}
				}
			});
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
