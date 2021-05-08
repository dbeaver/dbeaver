/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.vertica.model.VerticaSchema;
import org.jkiss.dbeaver.ext.vertica.model.VerticaSequence;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
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

import java.util.List;
import java.util.Map;

public class VerticaSequenceManager extends SQLObjectEditor<VerticaSequence, VerticaSchema> implements DBEObjectRenamer<VerticaSequence> {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }
    @Override
    protected VerticaSequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        VerticaSchema schema = (VerticaSchema) structContainer.getSchema();
        VerticaSequence sequence = new VerticaSequence(structContainer, "new_sequence");
        setNewObjectName(monitor, schema, sequence);
        return sequence;
    }
    protected String getBaseObjectName() {
        return "new_sequence";
    }
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction(
            "Create sequence",
            "CREATE SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)
        ));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        actions.add(
            new SQLDatabasePersistAction("Drop sequence", "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, VerticaSequence> getObjectsCache(VerticaSequence object) {
        return null;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, VerticaSequence object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        VerticaSequence sequence = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename sequence",
                "ALTER SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(sequence.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        VerticaSequence sequence = command.getObject();
        final StringBuilder sequenceOptions = new StringBuilder();
        addSequenceOptions(sequenceOptions, command.getProperties());

        if (sequenceOptions.length() > 0) {
            actionList.add(new SQLDatabasePersistAction(
                "Alter sequence",
                "ALTER SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + sequenceOptions
            ));
        }
    }

    private void addSequenceOptions(StringBuilder ddl, Map<Object, Object> options) {
        if (options.containsKey("cacheCount")) {
            ddl.append("\n\tCACHE ").append(options.get("cacheCount"));
        }
        if (options.containsKey("cycle")) {
            ddl.append("\n\t");
            if (!CommonUtils.toBoolean(options.get("cycle"))) {
                ddl.append("NO ");
            }
            ddl.append("CYCLE");
        }
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, NestedObjectCommand<VerticaSequence, PropertyHandler> command, Map<String, Object> options) {
        VerticaSequence sequence = command.getObject();
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment sequence",
                "COMMENT ON SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(sequence, sequence.getDescription())));
        }
    }
}
