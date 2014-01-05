package anders.akita.plan;

public class MapJoinOperator extends FetchDataOperator{

	public FetchDataOperator leftSrc;
	
	//String[] rhsEntries; 
	
	public String collectNode;
	
	public boolean genPrevID; //whether this step need generate ID when fetch previous step output tab
	
	//String midTab;
	
	//String joinClause;
	
	//boolean genIDForResTab;
}
