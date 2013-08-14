package anders.akita.parser;

import java.util.*;

public class ZSwitchExpr extends ZExp {

	Vector<ZExp> cond, result;
	ZExp cmpVal, else_result;

	public ZSwitchExpr(Vector<ZExp> cond, Vector<ZExp> result, ZExp cmpVal, ZExp else_result) {
		this.cmpVal = cmpVal;
		this.else_result = else_result;
		this.cond = cond;
		this.result = result;
	}

	public Vector<ZExp> getCond() {
		return cond;
	}

	public Vector<ZExp> getResult() {
		return result;
	}

	public ZExp getCmpVal() {
		return cmpVal;
	}

	public ZExp getElseResult() {
		return else_result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CASE ");
		if (cmpVal != null)
			sb.append(cmpVal.toString()).append(' ');
		for (int i = 0; i < cond.size(); ++i) {
			sb.append("WHEN ").append(((ZExp) cond.get(i)).toString())
					.append(' ');
			sb.append("THEN ").append(((ZExp) result.get(i)).toString())
					.append(' ');
		}
		if (else_result != null) {
			sb.append("ELSE ").append(else_result.toString()).append(' ');
		}
		sb.append("END");
		return sb.toString();
	}
}
