/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * OracleViewManager
 */
public class OracleViewManager extends SQLTableManager<OracleView, OracleSchema> {

    private static final Class<?>[] CHILD_TYPES = {
        OracleTableConstraint.class,
        OracleTableForeignKey.class,
    };

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command, Map<String, Object> options)
        throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty");
        }
//        if (CommonUtils.isEmpty(command.getObject().getViewText())) {
//            throw new DBException("View definition cannot be empty");
//        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleView> getObjectsCache(OracleView object) {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_VIEW_NAME;
    }

    @Override
    protected OracleView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) {
        OracleSchema schema = (OracleSchema) container;
        OracleView newView = new OracleView(schema, "NEW_VIEW"); //$NON-NLS-1$
        setNewObjectName(monitor, schema, newView);
        newView.setViewText("CREATE OR REPLACE VIEW " + newView.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\nSELECT");
        return newView;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceViewQuery(monitor, actions, command);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceViewQuery(monitor, actionList, command);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, DBECommandComposite<OracleView, PropertyHandler> command) throws DBException {
        final OracleView view = command.getObject();
        boolean hasComment = command.getProperty("comment") != null;
        if (!hasComment || command.getProperties().size() > 1) {
            String viewText = view.getViewText().trim();
            while (viewText.endsWith(";")) {
                viewText = viewText.substring(0, viewText.length() - 1);
            }
            actions.add(new SQLDatabasePersistAction("Create view", viewText));
        }
        String comment = view.getComment(monitor);
        if (!CommonUtils.isEmpty(comment)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS '" + view.getComment() + "'"));
        }
    }

}

