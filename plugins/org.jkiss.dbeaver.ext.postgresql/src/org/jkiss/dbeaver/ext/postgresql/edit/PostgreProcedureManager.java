/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreProcedureManager
 */
public class PostgreProcedureManager extends SQLObjectEditor<PostgreProcedure, PostgreSchema> implements DBEObjectRenamer<PostgreProcedure> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreProcedure> getObjectsCache(PostgreProcedure object)
    {
        return object.getContainer().proceduresCache;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Function name cannot be empty");
        }
    }

    @Override
    protected PostgreProcedure createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final PostgreSchema parent, Object copyFrom)
    {
        return new UITask<PostgreProcedure>() {
            @Override
            protected PostgreProcedure runTask() {
                CreateFunctionPage editPage = new CreateFunctionPage(parent, monitor);
                if (!editPage.edit()) {
                    return null;
                }
                PostgreProcedure newProcedure = new PostgreProcedure(parent);
                if (editPage.getPredefinedProcedureType() == DBSProcedureType.FUNCTION) {
                    newProcedure.setKind(PostgreProcedureKind.f);
                    newProcedure.setReturnType(editPage.getReturnType());
                } else {
                    newProcedure.setKind(PostgreProcedureKind.p);
                }
                newProcedure.setName(editPage.getProcedureName());
                newProcedure.setLanguage(editPage.getLanguage());
                return newProcedure;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        String objectType = command.getObject().getProcedureTypeName();
        actions.add(
            new SQLDatabasePersistAction("Drop function", "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, PostgreProcedure procedure)
    {
        actions.add(
            new SQLDatabasePersistAction("Create function", procedure.getBody(), true));
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, NestedObjectCommand<PostgreProcedure, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment function",
                "COMMENT ON " + command.getObject().getProcedureTypeName() + " " + command.getObject().getFullQualifiedSignature() +
                    " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        if (isDDL) {
            try {
                PostgreUtils.getObjectGrantPermissionActions(monitor, command.getObject(), actions, options);
            } catch (DBException e) {
                log.error(e);
            }
        }

    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreProcedure object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        PostgreProcedure procedure = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename function",
                "ALTER " + command.getObject().getProcedureTypeName() + " " +
                        DBUtils.getQuotedIdentifier(procedure.getSchema()) + "." + PostgreProcedure.makeOverloadedName(procedure.getSchema(), command.getOldName(), procedure.getParameters(monitor), true) +
                    " RENAME TO " + DBUtils.getQuotedIdentifier(procedure.getDataSource(), command.getNewName()))
        );
    }

    private static class CreateFunctionPage extends CreateProcedurePage {
        private final PostgreSchema parent;
        private final DBRProgressMonitor monitor;
        private PostgreLanguage language;
        private PostgreDataType returnType;
        private Combo returnTypeCombo;

        public CreateFunctionPage(PostgreSchema parent, DBRProgressMonitor monitor) {
            super(parent);
            this.parent = parent;
            this.monitor = monitor;
        }

        @Override
        public DBSProcedureType getPredefinedProcedureType() {
            if (parent.getDataSource().isServerVersionAtLeast(11, 0)) {
                return null;
            }
            return DBSProcedureType.FUNCTION;
        }

        @Override
        protected void updateProcedureType(DBSProcedureType type) {
            returnTypeCombo.setEnabled(type.hasReturnValue());
        }

        @Override
        protected void createExtraControls(Composite group) {
            {
                List<PostgreLanguage> languages = new ArrayList<>();
                try {
                    languages.addAll(parent.getDatabase().getLanguages(monitor));
                } catch (DBException e) {
                    log.error(e);
                }
                final Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN | SWT.READ_ONLY);
                for (PostgreLanguage lang : languages) {
                    languageCombo.add(lang.getName());
                }

                languageCombo.addModifyListener(e -> {
                    language = languages.get(languageCombo.getSelectionIndex());
                });
                languageCombo.setText("sql");
            }
            {
                List<PostgreDataType> dataTypes = new ArrayList<>(parent.getDatabase().getDataSource().getLocalDataTypes());
                returnTypeCombo = UIUtils.createLabelCombo(group, "Return type", SWT.DROP_DOWN);
                for (PostgreDataType dt : dataTypes) {
                    returnTypeCombo.add(dt.getName());
                }

                returnTypeCombo.addModifyListener(e -> {
                    String dtName = returnTypeCombo.getText();
                    if (!CommonUtils.isEmpty(dtName)) {
                        returnType = parent.getDatabase().getDataSource().getLocalDataType(dtName);
                    } else {
                        returnType = null;
                    }
                });
                returnTypeCombo.setText("int4");
            }
            
        }

        public PostgreLanguage getLanguage() {
            return language;
        }

        public PostgreDataType getReturnType() {
            return returnType;
        }
    }
}

