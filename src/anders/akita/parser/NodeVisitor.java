package anders.akita.parser;

public interface NodeVisitor {
	public void visit(ZExp node, RootExp root)
			throws ExecException;
}
