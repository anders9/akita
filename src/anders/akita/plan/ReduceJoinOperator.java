package anders.akita.plan;

public class ReduceJoinOperator extends FetchDataOperator {

	FetchDataOperator[] srcs;
	
	int[][] joinKeyIdx;
	
	boolean genPrevID; //whether this step need generate ID when fetch previous step output tab
}
