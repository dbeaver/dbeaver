package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGFinder;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PostgreFinder implements DBGFinder {
    
    private final PostgreDataSource dataSource;
    
    public PostgreFinder(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObject findObject(Map<String, Object> context, Object identifier, DBRProgressMonitor monitor) throws DBException {
        Long oid = null;
        final String errorIdentifier = String.format("Unknown procedure identifier %s", identifier);
        if (identifier instanceof Number) {
            Number number = (Number) identifier;
            oid = number.longValue();
        } else if (identifier instanceof String) {
            String string = (String) identifier;
            try {
                oid = Long.parseLong(string);
            } catch (NumberFormatException e) {
                throw new DBException(errorIdentifier, e, dataSource);
            }
        }
        if (oid == null) {
            throw new DBException(errorIdentifier);
        }
        String databaseName = String.valueOf(context.get(DBGController.DATABASE_NAME));
        PostgreDatabase database = dataSource.getDatabase(databaseName);
        if (database == null) {
            return null;
        }
        String schemaName = String.valueOf(context.get(DBGController.SCHEMA_NAME));
        PostgreSchema schema = null;
        schema = database.getSchema(monitor, schemaName);
        if (schema == null) {
            return null;
        }
        PostgreProcedure procedure = schema.getProcedure(monitor, oid);
        return procedure;
    }

}
