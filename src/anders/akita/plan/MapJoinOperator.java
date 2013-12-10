package anders.akita.plan;

public class MapJoinOperator extends FetchDataOperator{

	FetchDataOperator leftSrc;
	
	String[] rhsEntries; //may be null
	
	String collectNode;
	
	String midTab;
	
	String joinClause;
	
	boolean containID;
	//boolean genMidTabID;
}
