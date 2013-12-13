package anders.akita.plan;

import java.util.*;

public class FetchDataOperator {

	String[] entries;
	
	Schema schema;
	
	String fetchSQL; // if null, use: select * from schema.name;
	
	ArrayList<String> nonRelSubQVar;
	
	//boolean containID; // move into schema
	
	ArrayList<String> tmpTabList;//temp tab generated in this step, on each Entry-Node listed in $entries
}
