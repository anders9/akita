package anders.akita.parser;

import java.util.ArrayList;

public class RootExp extends ZExp {

	ZExp exp;
	
	ArrayList<String> nonRelSubQVar;
	
	public RootExp(ZExp exp){
		this.exp = exp;
	}
	
	public Iterable<ZExp> subExpSet() {
		ArrayList<ZExp> list = new ArrayList<ZExp>(1);
		list.add(exp);
		return list;
	}

	public boolean replaceSubExp(ZExp oldExp, ZExp newExp) {
		if(exp == oldExp){
			exp = newExp;
			return true;
		}
		return false;
	}

	private void traverse(ZExp node, NodeVisitor v)
			throws ExecException
	{
		
		//node.root = this;
		
		Iterable<ZExp> iter = node.subExpSet();
		if(iter != null){
			for(ZExp sube: iter){
				sube.parentExp = node;
				traverse(sube, v);
			}
		}
		
		v.visit(node, this);
	}
	
	public void traverse(NodeVisitor visitor)
			throws ExecException
	{
		exp.parentExp = this;
		traverse(exp, visitor);
	}
	
	public String toString(){
		return exp.toString();
	}
}
