package anders.akita.exec;

public interface NodeEntry {
	
	public IRecordStream execQuery(String sql);
	
	IInsertBuff insertRecord(ITable midTab);
	
}
