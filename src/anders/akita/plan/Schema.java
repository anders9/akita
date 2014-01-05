package anders.akita.plan;

public class Schema implements Cloneable{

	public String name;
	
	public String[] col;
	
	public String[] type;
	
	public boolean containID = false;
	
	public Object clone(){
		Schema s = new Schema();
		s.name = name;
		s.col = col.clone();
		s.type = type.clone();
		s.containID = containID;
		return s;
	}
}
