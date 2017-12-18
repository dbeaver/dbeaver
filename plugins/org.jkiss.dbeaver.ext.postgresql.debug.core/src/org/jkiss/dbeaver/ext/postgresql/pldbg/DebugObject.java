package org.jkiss.dbeaver.ext.postgresql.pldbg;

public interface DebugObject<OBJECTID> {
	
	OBJECTID getID();
	String getName();

}
