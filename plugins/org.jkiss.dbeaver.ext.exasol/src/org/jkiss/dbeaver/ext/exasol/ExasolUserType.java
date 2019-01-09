package org.jkiss.dbeaver.ext.exasol;

public enum ExasolUserType {
	
	KERBEROS("kerberos"),
	LDAP("ldap"),
	LOCAL("local");

    private final String name;
	
	ExasolUserType(String name)
	{
		this.name = name;
		
	}
	
	public String getName()
	{
		return name;
	}
}
