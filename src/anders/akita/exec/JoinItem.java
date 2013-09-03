package anders.akita.exec;


import anders.akita.meta.*;

public class JoinItem {

	static public final int MAP_JOIN = 1;
	static public final int REDUCE_JOIN = 2;

	int optType;
	int reducerN;

	String alias;
	String table;
	QueryBlock subQB;

	public boolean existField(String field) {
		if (subQB != null) {
			return subQB.fieldList.containsKey(field);
		} else {
			return Meta.containCol(table, field);
		}
	}

	public String[] getFieldList() {
		if (subQB != null) {
			return subQB.selectAlias;
		} else {
			return Meta.getTab(table).getAllColName();
		}
	}

	public boolean isDistributed(){
		if (subQB != null)
			return true;
		else
			return Meta.getTab(table).isDistributed();
	}
	
}
