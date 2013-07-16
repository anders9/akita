package org.gibello.zql;

public class ZFromItemEx {

	ZQuery subQuery = null;
	ZFromItem fromItem = null;
	
	public boolean isSubQuery() {
		return subQuery != null;
	}

	public ZQuery getSubQuery(){
		return subQuery;
	}
	
	public ZFromItem getFromItem(){
		return fromItem;
	}
	
	public ZFromItemEx(ZFromItem item){
		this.fromItem = item;
	}
	
	public ZFromItemEx(ZQuery query){
		this.subQuery = query;
	}
	
	public String toString(){
		if(isSubQuery()){
			String r = "(" + subQuery.toString() + ")";
			if(subQuery.getAlias() != null)
				r += (" AS " + subQuery.getAlias());
			return r;
		}
		return fromItem.toString();
	}
}
