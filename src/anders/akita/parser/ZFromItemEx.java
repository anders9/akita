package anders.akita.parser;

import java.util.*;

import anders.akita.meta.*;

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
	
	  
	  public boolean existField(String field){
		  if(isSubQuery()){
			  return subQuery.fieldList.containsKey(field);
		  }
		  else{
			  return Meta.containCol(table, field);
		  }
	  }	
	
	public String[] getFieldList(){
		if(isSubQuery()){
			ArrayList<String> list = new ArrayList<String>();
			for(ZSelectItem item: subQuery.getSelect()){
				if(item.alias != null)
					list.add(item.alias);
				else if(item.expr instanceof ZColRef)
					list.add(((ZColRef) item.expr).col);
			}
			return list.toArray(new String[0]);
		}
		else{
			return Meta.getTab(table).getAllColName();
		}
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
