package anders.akita.parser;

public class ZInterval implements ZExp {
	
	String type;
	ZExp exp;
	
	public ZInterval(String type, ZExp exp){
		this.type = type;
		this.exp = exp;
	}
	
	public ZExp getExpr(){
		return exp;
	}
	public String toString(){
		return "Interval " + exp.toString() + " " + type;
	}
	
}
