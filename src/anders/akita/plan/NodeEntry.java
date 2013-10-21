package anders.akita.plan;

public interface NodeEntry {
	
	public IRecordStream execQuery(String sql);
	
	public IInsertBuff insertRecord(ITable midTab);
	
	public void deleteTab(String midTab);
	
}
