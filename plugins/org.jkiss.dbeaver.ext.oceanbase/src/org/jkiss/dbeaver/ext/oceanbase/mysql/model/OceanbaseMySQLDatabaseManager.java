package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class OceanbaseMySQLDatabaseManager extends SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource> implements DBEObjectRenamer<OceanbaseMySQLCatalog>{

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, OceanbaseMySQLCatalog> getObjectsCache(OceanbaseMySQLCatalog object) {
		return ((OceanbaseMySQLDataSource)object.getDataSource()).getOceanbaseCatalogCache();
	}

	@Override
	public void renameObject(DBECommandContext commandContext, OceanbaseMySQLCatalog object,
			Map<String, Object> options, String newName) throws DBException {
        throw new DBException("Direct database rename is not yet implemented in MySQL. You should use export/import functions for that.");
	}

	@Override
	protected OceanbaseMySQLCatalog createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		System.out.println("11111");
        return new OceanbaseMySQLCatalog((OceanbaseMySQLDataSource) container, null);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		final OceanbaseMySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
        appendDatabaseModifiers(catalog, script);
        actions.add(
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        );		
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) throws DBException {
		actions.add(new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`"));		
	}
	
	private void appendDatabaseModifiers(OceanbaseMySQLCatalog catalog, StringBuilder script) {
        if (catalog.getAdditionalInfo().getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getAdditionalInfo().getDefaultCharset().getName());
        }
        if (catalog.getAdditionalInfo().getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getAdditionalInfo().getDefaultCollation().getName());
        }
    }

}
