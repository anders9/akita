package anders.akita.parser;

import anders.util.Util;


public class ZColRef extends ZExp {
	
	static public String[] rawSrc;
	
	public String table;
	public String col;
	
	public ZColRef(String table, String col){
		
		this.table = table;
		this.col = col;
	}

	public String toString(){
		if(table == null)return col;
		
		if(rawSrc == null)
			return table + "." + col;
		else if(Util.findStr(table, rawSrc) != -1)
			return table + "." + col;
		else
			return table + "$" + col;
	}
	
	
	public Iterable<ZExp> subExpSet(){
		return null;
	}
	
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp){
		return false;
	}
}
