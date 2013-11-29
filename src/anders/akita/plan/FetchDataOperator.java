package anders.akita.plan;

import java.util.*;

public class FetchDataOperator {

	String[] entries;
	
	//String[] schema;
	//int[] colLen;
	
	Schema schema;
	
	String fetchSQL;
	//boolean genID;
	
	ArrayList<String> tmpTabList;//used for clean-up tmp table(generated in Operator)
}
