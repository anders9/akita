package anders.akita.exec;

import java.util.*;

public class QueryBlock implements ITable {

	String alias;
	
	boolean isRoot;
	
	QueryBlock parent;
	
	ArrayList<QueryBlock> children;
	
	ArrayList<String> refTabs;
	
	SubQueryBlock subQBLink;
	
	public void cleanup(){}
}
