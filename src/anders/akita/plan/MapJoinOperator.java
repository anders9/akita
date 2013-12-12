package anders.akita.plan;

public class MapJoinOperator extends FetchDataOperator{

	FetchDataOperator leftSrc;
	
	//String[] rhsEntries; 
	
	String collectNode;
	
	boolean genPrevID; //whether this step need generate ID when fetch previous step output tab
	
	//String midTab;
	
	//String joinClause;
	
	//boolean genIDForResTab;
}
