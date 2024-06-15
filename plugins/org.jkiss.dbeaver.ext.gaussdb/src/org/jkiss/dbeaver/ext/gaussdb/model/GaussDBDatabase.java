package org.jkiss.dbeaver.ext.gaussdb.model;

public class GaussDBDatabase extends PostgreDatabase {

	private static final Log log = Log.getLog(GaussDBDatabase.class);

	private DBRProgressMonitor monitor;

	private String characterType;

	private String databaseCompatibleMode;

	private boolean isPackageSupported;

}