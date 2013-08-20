package anders.akita.parser;

import java.util.ArrayList;

public class ZInterval extends ZExp {
	
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
		return "( INTERVAL " + exp.toString() + " " + type + " )";
	}
	
	public Iterable<ZExp> subExpSet(){
		ArrayList<ZExp> list = new ArrayList<ZExp>(1);
		list.add(exp);
		return list;
	}
	
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp){
		if(exp == oldExp){
			exp = newExp;
			return true;
		}
		return false;
	}
	
}
