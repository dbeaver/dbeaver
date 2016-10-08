package org.jkiss.dbeaver.ext.exasol.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

public enum ExasolScriptLanguage implements DBPNamedObject {
	
	R("R"),
	LUA("LUA"),
	JAVA("JAVA");
	
	private String name;

	private ExasolScriptLanguage(String name) {
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	@NotNull
	public String getName() {
		return name;
	}
	
	
	

}
