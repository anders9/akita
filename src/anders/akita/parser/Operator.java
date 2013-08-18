package anders.akita.parser;

public enum Operator {

	OR("OR"),
	AND("AND"),
	NOT("NOT"),
	EXISTS("EXISTS"),
	NOT_EXISTS("NOT EXISTS"),
	//SWITCH("SWITCH"),
	BETWEEN("BETWEEN"),
	NOT_BETWEEN("NOT BETWEEN"),
	LESS("<"),
	MORE(">"),
	LESS_EQ("<="),
	MORE_EQ(">="),
	EQ("="),
	NOT_EQ("!="),
	SAFE_EQ("<=>"),
	IS_NULL("IS NULL"),
	IS_NOT_NULL("IS NOT NULL"),
	LIKE("LIKE"),
	NOT_LIKE("NOT LIKE"),
	RLIKE("RLIKE"),
	NOT_RLIKE("NOT RLIKE"),
	IN("IN"),
	NOT_IN("NOT IN"),
	
	BIT_OR("|"),
	BIT_AND("&"),
	BIT_SHL("<<"),
	BIT_SHR(">>"),
	
	
	ADD("+"),
	SUB("-"),
	MUL("*"),
	DIV("/"),
	MOD("%"),
	BIT_XOR("^"),
	NEG("-"),
	INV("~"),
	
	NOT_HIGH_PREC("!"),
	;
	
	private String op;
	private Operator(String op){
		this.op = op;
	}
	public String op(){
		return op;
	}
}
