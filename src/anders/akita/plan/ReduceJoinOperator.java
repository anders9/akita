package anders.akita.plan;

public class ReduceJoinOperator extends FetchDataOperator {

	public FetchDataOperator[] srcs;
	
	public int[][] joinKeyIdx;
	
	public boolean genPrevID; //whether this step need generate ID when fetch previous step output tab
}
