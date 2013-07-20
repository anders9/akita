package anders.akita.parser;

import java.util.*;

public class ZFromClause {

	Vector items;
	ZExp join_cond;
	int join_type;
	public static final int INNER_JOIN = 1;
	public static final int LEFT_JOIN = 2;
	
	
	public ZFromClause(int join_type, Vector items, ZExp join_cond){
		this.join_cond = join_cond;
		this.join_type = join_type;
		this.items = items;
	}
	
}
