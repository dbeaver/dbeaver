package org.jkiss.dbeaver.postgresql.debug;

public interface DebugObject<OBJECTID> {
	
	OBJECTID getID();
	String getName();

}
