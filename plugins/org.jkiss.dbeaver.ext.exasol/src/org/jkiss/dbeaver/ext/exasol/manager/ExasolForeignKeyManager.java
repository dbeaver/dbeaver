package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableUniqueKey;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolCreateForeignKeyDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.ui.UITask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExasolForeignKeyManager
    extends SQLForeignKeyManager<ExasolTableForeignKey, ExasolTable> implements DBEObjectRenamer<ExasolTableForeignKey> {

    @Override
    public DBSObjectCache<? extends DBSObject, ExasolTableForeignKey> getObjectsCache(
        ExasolTableForeignKey object) {
        final ExasolTable parent = object.getParentObject();
        return parent.getContainer().getAssociationCache();
    }

    @Override
    protected ExasolTableForeignKey createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context,
        Object container, Object copyFrom, Map<String, Object> options) throws DBException {

        ExasolTable table = (ExasolTable) container;
        final ExasolTableForeignKey foreignKey = new ExasolTableForeignKey(
            table,
            null,
            true,
            "FK"
        );
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));

        return new UITask<ExasolTableForeignKey>() {
            @Override
            protected ExasolTableForeignKey runTask() {
                ExasolCreateForeignKeyDialog editPage = new ExasolCreateForeignKeyDialog("Create Foreign Key", foreignKey);

                if (!editPage.edit()) {
                    return null;
                }

                List<ExasolTableForeignKeyColumn> columns = new ArrayList<>();
                int cnt = 0;
                for (ExasolCreateForeignKeyDialog.FKColumnInfo column : editPage.getColumns()) {
                    try {
                        ExasolTable refTable = foreignKey.getReferencedConstraint().getTable();
                        columns.add(new ExasolTableForeignKeyColumn(
                            foreignKey,
                            table.getAttribute(monitor, column.getOwnColumn().getName()),
                            refTable.getAttribute(monitor, column.getRefColumn().getName()),
                            ++cnt));
                    } catch (DBException e) {
                        log.error("Could not get Attribute Information from Table");
                        return null;
                    }
                }

                foreignKey.setName(editPage.getName());
                foreignKey.setReferencedConstraint((ExasolTableUniqueKey)editPage.getUniqueConstraint());
                foreignKey.setEnabled(editPage.isEnabled());
                foreignKey.setColumns(columns);

                return foreignKey;
            }
        }.execute();
    }

    @Override
    protected String getDropForeignKeyPattern(ExasolTableForeignKey constraint) {
        return "ALTER TABLE " + DBUtils.getObjectFullName(constraint.getTable(), DBPEvaluationContext.DDL) + " DROP CONSTRAINT "
            + DBUtils.getObjectFullName(constraint, DBPEvaluationContext.DDL)
            ;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options) {
        final ExasolTableForeignKey key = command.getObject();

        try {
            actions.add(new SQLDatabasePersistAction("Create Foreign Key", ExasolUtils.getFKDdl(key, monitor)));
        } catch (DBException e) {
            log.error("Could not created DDL for Exasol FK: " + key.getFullyQualifiedName(DBPEvaluationContext.DDL));
            log.error(e.getMessage());
        }


    }


    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options) {
        final ExasolTableForeignKey key = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename FK",
                "ALTER TABLE " + DBUtils.getObjectFullName(key.getTable(), DBPEvaluationContext.DDL) + " RENAME CONSTRAINT "
                    + DBUtils.getQuotedIdentifier(key.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(key.getDataSource(), command.getNewName())
            )
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options) {
        final ExasolTableForeignKey constraint = command.getObject();

        if (command.getProperties().containsKey("enabled")) {
            actionList.add(
                new SQLDatabasePersistAction("Alter FK",
                    "ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " MODIFY CONSTRAINT " + constraint.getName() + " " +
                        (constraint.getEnabled() ? "ENABLE" : "DISABLE")
                )
            );
        }
    }

    @Override
    protected void processObjectRename(DBECommandContext commandContext, ExasolTableForeignKey object, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }


    @Override
    public void renameObject(DBECommandContext commandContext,
                             ExasolTableForeignKey object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

}
