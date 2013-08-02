package anders.akita.meta;

import anders.util.*;

public final class Table {

	String name;
	
	String[] colName;
	
	Column[] col;
	
	boolean isDistr;
	
	public boolean isDistributed(){
		return isDistr;
	}
	
	public Column getCol(String colName){
		int idx = Util.findStr(colName, this.colName);
		if(idx != -1)
			return this.col[idx];
		return null;
	}
	
	public String[] getAllColName(){
		return colName.clone();
	}
	
	public int colNum(){
		return col.length;
	}
	
	public Column getCol(int index){
		return col[index];
	}
	
	public boolean containCol(String colName){
		return -1 != Util.findStr(colName, this.colName);
	}
	
}
