package org.jkiss.dbeaver.ext.dm.tasks;

import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;

public class DmSchemaExportInfo {
	
	@NotNull
	private DmSchema schema;
	
    @Nullable
    private Collection<DmTableBase> tables;

	public DmSchemaExportInfo(DmSchema schema, Collection<DmTableBase> tables) {
		this.schema = schema;
		this.tables = tables;
	}

	
    @NotNull
    public DmSchema getDatabase() {
        return schema;
    }

    @Nullable
    public Collection<DmTableBase> getTables() {
        return tables;
    }

    @Override
    public String toString() {
        return schema.getName() + " " + tables;
    }
    
    
}
