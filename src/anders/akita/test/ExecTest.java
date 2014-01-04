package anders.akita.test;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import anders.akita.exec.Executor;
import anders.akita.meta.*;
import anders.akita.parser.*;

public class ExecTest {

	final static private Logger logger = Logger.getLogger(ExecTest.class);
	
	interface Handler{
		public void exec(ZStatement stmt);
	}
	private static void readStmt(String srcfile, Handler handler)
		throws IOException
	{	
			Scanner sc = new Scanner(new FileInputStream(srcfile));
			PrintStream os = new PrintStream(System.out);
			
			while (sc.hasNextLine()) {
				ZqlJJParser parser = null;
				try {
					String line = sc.nextLine().trim();

					if(line.equals("") || line.startsWith("//") )
						continue;
					
					os.println(line);
					
					parser = new ZqlJJParser(new StringBufferInputStream(line));
					ZStatement stmt = parser.SQLStatement();
					os.println(stmt.toString());
					handler.exec(stmt);
					
					os.println();
				} catch (ParseException pe) {
					os.println(pe.getMessage());
					parser.ReInit(System.in);
				} 
				catch (anders.akita.parser.TokenMgrError te) {
					os.println(te.getMessage());
					parser.ReInit(System.in);
				} 
			}
			sc.close();
			os.close();
	}
	
	public static void main(String[] args) {
		try{
			readStmt("indata/schema.txt", new Handler(){
				public void exec(ZStatement stmt) {
					if(stmt instanceof ZCreateTable){
						Meta.createTable((ZCreateTable)stmt);
					}
				}
			});
			readStmt("indata/query.txt", new Handler(){
				public void exec(ZStatement stmt) {
					if(stmt instanceof ZQuery){
						try{
							Executor.INSTANCE.exec((ZQuery)stmt);
						}catch(ExecException e){
							e.printStackTrace();
						}
					}
				}
			});
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
