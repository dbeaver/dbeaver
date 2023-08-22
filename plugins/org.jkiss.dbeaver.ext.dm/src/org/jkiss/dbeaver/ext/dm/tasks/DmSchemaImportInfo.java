package org.jkiss.dbeaver.ext.dm.tasks;



import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;

public class DmSchemaImportInfo {

	@NotNull
	private DmSchema schema;
	

	public DmSchemaImportInfo(DmSchema schema) {
		this.schema = schema;
	}

	
    @NotNull
    public DmSchema getDatabase() {
        return schema;
    }


    @Override
    public String toString() {
        return schema.getName();
    }
	
}
