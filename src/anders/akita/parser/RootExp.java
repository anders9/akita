package anders.akita.parser;

import java.util.ArrayList;

public class RootExp extends ZExp {

	ZExp exp;
	
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

	private void traverse(ZExp node, NodeVisitor v){
		
		node.root = this;
		
		Iterable<ZExp> iter = node.subExpSet();
		if(iter != null){
			for(ZExp sube: iter){
				sube.parentExp = node;
				traverse(sube, v);
			}
		}
		
		v.visit(node, this);
	}
	
	public void traverse(NodeVisitor visitor){
		exp.parentExp = this;
		traverse(exp, visitor);
	}
}
