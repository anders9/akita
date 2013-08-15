package anders.akita.parser;

import java.util.*;

public class ZFromClause {

	public Vector<ZFromItemEx> items;
	public ZExp join_cond;
	public int join_type;
	
	public static final int INNER_JOIN = 1;
	public static final int LEFT_JOIN = 2;
	public static final int RIGHT_JOIN = 3;
	
	public int getItemN(){
		return items.size();
	}
	public ZFromItemEx getItem(int idx){
		return (ZFromItemEx)items.get(idx);
	}
	
	public ZFromClause(int join_type, Vector<ZFromItemEx> items, ZExp join_cond){
		this.join_cond = join_cond;
		this.join_type = join_type;
		this.items = items;
	}
	
	public String toString(){
		StringBuilder buf = new StringBuilder();
	    buf.append("from ");
	    if(join_type == ZFromClause.INNER_JOIN){
		    buf.append(items.elementAt(0).toString());
		    for(int i=1; i<items.size(); i++) {
		      buf.append(", " + items.elementAt(i).toString());
		    }
	    }
	    else if(join_type == ZFromClause.LEFT_JOIN || join_type == ZFromClause.RIGHT_JOIN){
	    	buf.append(items.get(0).toString())
	    	.append(join_type == ZFromClause.LEFT_JOIN ?" LEFT JOIN ": " RIGHT JOIN ")
	    	.append(items.get(1).toString());
	    	if(join_cond != null){
	    		buf.append(" ON ")
	    		.append(join_cond.toString());
	    	}
	    }
	    return buf.toString();
	}
}
