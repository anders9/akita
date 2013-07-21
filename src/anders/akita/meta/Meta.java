package anders.akita.meta;

import java.util.*;

public final class Meta {

	//static final Meta INSTANCE = new Meta();
	
	static HashMap<String, Table> tables;
	
	static public Table getTab(String tab){
		return tables.get(tab);
	}
	
	static public boolean containCol(String tab, String col){
		return tables.get(tab).containCol(col);
	}
	
}
