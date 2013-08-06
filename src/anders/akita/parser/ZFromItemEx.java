package anders.akita.parser;

public class ZFromItemEx {

	public String alias;
	public String table;
	
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
		String s;
		if(isSubQuery()){
			s = "(" + subQuery.toString() + ")";
		}
		else{
			s = fromItem.toString();
		}
		if(alias != null){
			s += (" AS " + alias);
		}
		return s;
	}
}
