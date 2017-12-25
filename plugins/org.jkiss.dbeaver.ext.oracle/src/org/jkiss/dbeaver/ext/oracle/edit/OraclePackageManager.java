/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * OraclePackageManager
 */
public class OraclePackageManager extends SQLObjectEditor<OraclePackage, OracleSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OraclePackage> getObjectsCache(OraclePackage object)
    {
        return object.getSchema().packageCache;
    }

    @Override
    protected OraclePackage createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final OracleSchema parent, Object copyFrom)
    {
        return new UITask<OraclePackage>() {
            @Override
            protected OraclePackage runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.PACKAGE);
                if (!editPage.edit()) {
                    return null;
                }
                String packName = editPage.getEntityName();
                OraclePackage oraclePackage = new OraclePackage(
                    parent,
                    packName);
                oraclePackage.setObjectDefinitionText(
                    "CREATE OR REPLACE PACKAGE " + packName + "\n" +
                    "AS\n" +
                    "-- Package header\n" +
                    "END " + packName +";");
                oraclePackage.setExtendedDefinitionText(
                    "CREATE OR REPLACE PACKAGE BODY " + packName + "\n" +
                        "AS\n" +
                        "-- Package body\n" +
                        "END " + packName +";");
                return oraclePackage;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options)
    {
        final OraclePackage object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop package",
                "DROP PACKAGE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actionList, objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, OraclePackage pack)
    {
        try {
            String header = pack.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS);
            if (!CommonUtils.isEmpty(header)) {
                actionList.add(
                    new OracleObjectValidateAction(
                        pack, OracleObjectType.PACKAGE,
                        "Create package header",
                        header)); //$NON-NLS-1$
            }
            String body = pack.getExtendedDefinitionText(new VoidProgressMonitor());
            if (!CommonUtils.isEmpty(body)) {
                actionList.add(
                    new OracleObjectValidateAction(
                        pack, OracleObjectType.PACKAGE_BODY,
                        "Create package body",
                        body));
            } else {
                actionList.add(
                    new SQLDatabasePersistAction(
                        "Drop package header",
                        "DROP PACKAGE BODY " + pack.getFullyQualifiedName(DBPEvaluationContext.DDL),
                        DBEPersistAction.ActionType.OPTIONAL) //$NON-NLS-1$
                    );
            }
        } catch (DBException e) {
            log.warn(e);
        }
        OracleUtils.addSchemaChangeActions(actionList, pack);
    }

}

