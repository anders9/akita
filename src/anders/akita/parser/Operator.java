package anders.akita.parser;

public enum Operator {

	OR("OR"),
	AND("AND"),
	NOT("NOT"),
	EXISTS("EXISTS"),
	NOT_EXISTS("NOT EXISTS"),
	//SWITCH("SWITCH"),
	
	
	;
	
	private String op;
	private Operator(String op){
		this.op = op;
	}
	public String op(){
		return op;
	}
}
