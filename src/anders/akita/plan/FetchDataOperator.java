package anders.akita.plan;

import java.util.*;

public class FetchDataOperator {

	public String[] entries;
	
	public Schema schema;
	
	public String fetchSQL; // if null, use: select * from schema.name;
	
	public ArrayList<String> nonRelSubQVar;
	
	//boolean containID; // move into schema
	
	public ArrayList<String> tmpTabList;//temp tab generated in this step, on each Entry-Node listed in $entries
}
