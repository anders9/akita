package anders.akita.test;


import anders.akita.parser.*;

public class SQLParserTest {

	public static void main(String[] args) {

		ZqlJJParser parser = new ZqlJJParser(System.in);
		while (true) {
			try {
				System.out.println("Input SQL below:");
				ZStatement zq = parser.SQLStatement();
				if(zq == null)
					break;
				System.out.println(zq.toString());
			} catch (ParseException pe) {
				System.out.println(pe.getMessage());
				parser.ReInit(System.in);
			} 
			catch (anders.akita.parser.TokenMgrError te) {
				System.out.println(te.getMessage());
				parser.ReInit(System.in);
			} 
		}
	}

}
