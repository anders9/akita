package anders.akita.parser;

import java.util.*;

public class ZFromClause {

	public Vector<ZFromItemEx> items;
	public ZExp join_cond;
	public int join_type;
	
	public static final int INNER_JOIN = 1;
	public static final int LEFT_JOIN = 2;
	
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
	
}
