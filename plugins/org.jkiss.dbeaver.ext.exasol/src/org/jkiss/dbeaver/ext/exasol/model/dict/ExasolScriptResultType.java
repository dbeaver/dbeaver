package org.jkiss.dbeaver.ext.exasol.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

public enum ExasolScriptResultType implements DBPNamedObject {
	
	TABLE("Table"),
	ROWCOUNT("Row Count"),
	RETURNS("Returns value"),
	EMITS("Emits Table");
	
	
	
	private String name;

	private ExasolScriptResultType(String name) {
		this.name = name;
	}
	
	@Override
	@NotNull
	public String toString()
	{
		return name;
	}
	

	@Override
	public String getName() {
		return name;
	}

}
