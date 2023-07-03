package org.jkiss.dbeaver.ext.mysql;

import org.eclipse.osgi.util.NLS;

public class MySQLMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mysql.MySQLMessages";
	
	public static String exception_direct_database_rename;
	public static String exception_only_select_could_produce_execution_plan;
	public static String table_column_length_tooltip;
	public static String table_column_length;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, MySQLMessages.class);
	}

	private MySQLMessages() {}

}
