package anders.akita.parser;

import java.util.*;

public class ZColRef extends ZExp {
	
	public String table;
	public String col;
	public ZQuery query;
	
	public ZColRef(String table, String col){
		
		this.table = table;
		this.col = col;
	}

	public String toString(){
		if(table == null)return col;
		return table + "." + col;
	}
	
	public Iterable<ZExp> subExpSet(){
		return new ArrayList<ZExp>();
	}
	
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp){
		return false;
	}
}
