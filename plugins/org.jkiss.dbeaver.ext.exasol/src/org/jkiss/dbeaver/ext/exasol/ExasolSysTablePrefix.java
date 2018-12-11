package org.jkiss.dbeaver.ext.exasol;

public enum ExasolSysTablePrefix {
	SESSION("EXA_SESSION"),
	DBA("EXA_DBA"),
	USER("EXA_USER"),
	ALL("EXA_ALL");
	
	private final String prefix;
	ExasolSysTablePrefix(String prefix)
	{
		this.prefix = prefix;
	}
	
	@Override
	public String toString() {
		return this.prefix;
	}

}
