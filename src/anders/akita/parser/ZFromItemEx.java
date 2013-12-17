package anders.akita.parser;

import java.util.*;

import anders.akita.meta.*;

import anders.akita.plan.*;

public class ZFromItemEx {

	JoinPolicy joinPolicy;
	int joinReducerN;
	MidTabStorageType[] mstType = new MidTabStorageType[]{MidTabStorageType.Memory, MidTabStorageType.Memory};
	
	public void setMidTabStorageType(MidTabStorageType t1, MidTabStorageType t2){
		mstType[0] = t1;
		mstType[1] = t2;
	}
	public String alias;
	//public String table;
	
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
	
	public ZFromItemEx(ZFromItem item, JoinPolicy joinPolicy, int joinReducerN){
		this.fromItem = item;
		this.joinPolicy = joinPolicy;
		this.joinReducerN = joinReducerN;
	}
	
	public ZFromItemEx(ZQuery query, JoinPolicy joinPolicy, int joinReducerN){
		this.subQuery = query;
		this.joinPolicy = joinPolicy;
		this.joinReducerN = joinReducerN;	}
	
	/*  
	  public boolean existField(String field){
		  if(isSubQuery()){
			  return subQuery.fieldList.containsKey(field);
		  }
		  else{
			  return Meta.containCol(fromItem.table, field);
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
			return Meta.getTab(fromItem.table).getAllColName();
		}
	}
	 */ 
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
