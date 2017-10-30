package org.jkiss.dbeaver.model.impl.jdbc.exec;


public class JDBCPreparedStatementCachedImpl extends JDBCPreparedStatementImpl {

	public JDBCPreparedStatementCachedImpl(JDBCPreparedStatementImpl statment){
		super(statment.connection, statment.original, statment.query, statment.disableLogging);
	}

	@Override
	public void close() {
		// For cached statement close() do nothing
	}
	
	public void drop(){
		super.close();
	}
	
}
