package anders.akita.plan;

public class LocalJoinOperator extends FetchDataOperator{

	FetchDataOperator leftSrc;
	
	boolean genPrevID; //whether this step need generate ID when fetch previous step output tab
	
}
