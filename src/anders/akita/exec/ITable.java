package anders.akita.exec;

import java.util.*;

public interface ITable {
	
	public String alias();

	public String uniqAlias();
	
	public ArrayList<String> embeddedAlias();
	
	public void cleanup();
	
	public int fieldNum();
	
	public String fieldAt(int index);
	
	public boolean isLocal();
	
	public int fragNum();
	
}
