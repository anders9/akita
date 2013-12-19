package anders.akita.parser;

public class ZPrepareCol extends ZExp{

	public ZPrepareCol(){
		
	}

	@Override
	public Iterable<ZExp> subExpSet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp) {
		throw new UnsupportedOperationException();
	}
}
