package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class CubridSynonymManager extends SQLObjectEditor<GenericSynonym, GenericStructContainer> implements DBEObjectRenamer<GenericSynonym> {

    public static final String BASE_SYNONYM_NAME = "new_synonym";

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GenericSynonym> getObjectsCache(GenericSynonym object) {
        DBSObject parentObject = object.getParentObject();
        if (parentObject instanceof GenericObjectContainer container) {
            return container.getSynonymCache();
        }
        return null;
    }

    @Override
    protected CubridSynonym createDatabaseObject(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            @Nullable Object container,
            @Nullable Object copyFrom,
            @NotNull Map<String, Object> options) {
        return new CubridSynonym((GenericStructContainer) container, BASE_SYNONYM_NAME);
    }

    public String buildStatement(@NotNull NestedObjectCommand command, @NotNull boolean isCreate) {
        StringBuilder query = new StringBuilder();
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        query.append(isCreate ? "CREATE SYNONYM " : "ALTER SYNONYM ");
        query.append(synonym.getOwner() + "." + synonym.getName());
        query.append(" FOR ").append(synonym.getTargetObject());
        if ((!synonym.isPersisted() && synonym.getDescription() != null) || command.hasProperty("description")) {
            query.append(" COMMENT ").append(SQLUtils.quoteString(synonym, CommonUtils.notEmpty(synonym.getDescription())));
        }
        return query.toString();
    }

    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Create Synonym", buildStatement(command, true)));
    }

    @Override
    protected void addObjectModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectChangeCommand command,
            @NotNull Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Modify Serial", buildStatement(command, false)));
    }

    @Override
    protected void addObjectDeleteActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectDeleteCommand command,
            @NotNull Map<String, Object> options) {
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop Synonym",
        "DROP SYNONYM " + synonym.getOwner() + "." + synonym.getName()));
    }

    @Override
    protected void addObjectRenameActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectRenameCommand command,
            @NotNull Map<String, Object> options) {
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        actions.add(new SQLDatabasePersistAction("Rename Synonym",
        "RENAME SYNONYM " + synonym.getOwner() + "." + command.getOldName() + " TO " + synonym.getOwner() + "." + command.getNewName()));
    }

    @Override
    public void renameObject(
            @NotNull DBECommandContext commandContext,
            @NotNull GenericSynonym object,
            @NotNull Map<String, Object> options,
            @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}
