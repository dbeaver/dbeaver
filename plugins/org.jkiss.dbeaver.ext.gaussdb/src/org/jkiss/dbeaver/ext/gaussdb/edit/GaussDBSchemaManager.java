package org.jkiss.dbeaver.ext.gaussdb.edit;

import java.util.Map;

import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreSchemaManager;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBSchemaManager extends PostgreSchemaManager {

    @Override
    protected PostgreSchema createDatabaseObject(DBRProgressMonitor monitor,
                                                 DBECommandContext context,
                                                 final Object container,
                                                 Object copyFrom,
                                                 Map<String, Object> options) {
        GaussDBDatabase database = (GaussDBDatabase) container;
        return database.createSchemaImpl(database, "NewSchema", (PostgreRole) null);
    }

}