package anders.akita.plan;

public class Schema implements Cloneable{

	String name;
	
	String[] col;
	
	String[] type;
	
	boolean containID = false;
	
	public Object clone(){
		Schema s = new Schema();
		s.name = name;
		s.col = col.clone();
		s.type = type.clone();
		s.containID = containID;
		return s;
	}
}
