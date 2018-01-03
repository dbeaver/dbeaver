package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class DebugSessionWorker implements Callable<DebugSessionResult> {

	private final Connection conn;
	private String sql ="";
	
	public void execSQL(String sqlCommand) {
		this.sql = sqlCommand;
	}

	public DebugSessionWorker(Connection conn) {
		this.conn = conn;
	}

	@Override
	public DebugSessionResult call() throws Exception {
		
		 try (Statement stmt = conn.createStatement()) {
    		 stmt.executeQuery(sql);
    		 return new DebugSessionResult(true,null);
    		 
    	 } catch (SQLException e) {
    		 return new DebugSessionResult(false,e);
	     }
	}
	
}
