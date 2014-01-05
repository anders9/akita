package anders.akita.plan;

public class AggrOperator extends FetchDataOperator {
	
	public FetchDataOperator src;
	
	public int[] shuffleKeyIdx;// if NULL, no shuffle process

}
