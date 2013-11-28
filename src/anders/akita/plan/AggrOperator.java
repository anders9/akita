package anders.akita.plan;

public class AggrOperator extends FetchDataOperator {
	
	FetchDataOperator src;
	
	int[] shuffleKeyIdx;// if NULL, no shuffle process

}
