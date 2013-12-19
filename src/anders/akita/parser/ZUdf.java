package anders.akita.parser;

import java.util.*;

public class ZUdf {

	private String name;

	private String[] parm;
	private boolean[] toAggr;
	private String exp;
	private boolean isAggr;

	public String getName() {
		return name;
	}

	public int getParmN() {
		return parm.length;
	}

	public boolean isAggr() {
		return isAggr;
	}

	public String getParm(int i) {
		return parm[i];
	}

	public boolean getParmAggr(int i) {
		return toAggr[i];
	}

	public String getExp() {
		return exp;
	}

	public ZUdf(String name, final ArrayList<String> parm, ZExp exp)
			throws ParseException {
		this.name = name;
		this.parm = parm.toArray(new String[0]);

		this.exp = exp.toString();
		this.toAggr = new boolean[this.parm.length];

		RootExp re = new RootExp(exp);

		try {
			re.traverse(new NodeVisitor() {
				public void visit(ZExp node, RootExp root) throws ExecException {
					if (node instanceof ZQuery){
						throw new ExecException("Cannot use inner-query in UDF: "
								+ root.toString());
					}
					if (node instanceof ZColRef) {
						ZColRef c = (ZColRef) node;
						if (c.table != null || c.col == null
								|| !parm.contains(c.col))
							throw new ExecException(
									"Invalid UDF function definition: col '"
											+ c.toString() + "' in: "
											+ ZUdf.this.exp);
						int aggrCnt = 0;
						while (node != null) {
							if (node instanceof ZExpression
									&& ((ZExpression) node).isAggr())
								++aggrCnt;
							node = node.parentExp;
						}
						if (aggrCnt > 1)
							throw new ExecException(
									"Invalid UDF function definition: col '"
											+ c.toString()
											+ "' is aggregated more than once in: "
											+ ZUdf.this.exp);
						int idx = parm.indexOf(c.col);
						isAggr = false;
						toAggr[idx] = aggrCnt == 1;
						if (aggrCnt == 1)
							isAggr = true;
					}
				}
			});
		} catch (ExecException e) {
			throw new ParseException(e.getMessage());
		}
	}

}
