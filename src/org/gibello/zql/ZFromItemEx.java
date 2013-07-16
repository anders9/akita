package org.gibello.zql;

public class ZFromItemEx {

	ZQuery subQuery;
	ZFromItem fromItem;
	
	public boolean isSubQuery() {
		return subQuery != null;
	}

	public ZQuery getSubQuery(){
		return subQuery;
	}
	public ZFromItem getFromItem(){
		return fromItem;
	}
	
	public ZFromItemEx(ZFromItem item, ZQuery subQuery){
		this.subQuery = subQuery;
		this.fromItem = item;
	}
	
	public String toString(){
		return isSubQuery() ? "(" + subQuery.toString() + ")" : fromItem.toString();
	}
}
