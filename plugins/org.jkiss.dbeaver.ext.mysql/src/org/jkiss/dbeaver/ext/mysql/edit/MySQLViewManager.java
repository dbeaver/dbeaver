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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * MySQLViewManager
 */
public class MySQLViewManager extends MySQLTableManager {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableBase> getObjectsCache(MySQLTableBase object)
    {
        return object.getContainer().getTableCache();
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        MySQLTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(((MySQLView) object).getAdditionalInfo().getDefinition())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Override
    protected MySQLView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, MySQLCatalog parent, Object copyFrom)
    {
        MySQLView newView = new MySQLView(parent);
        try {
            newView.setName(getNewChildName(monitor, parent, "new_view"));
        } catch (DBException e) {
            // Never be here
            log.error(e);
        }
        return newView;
    }

    @Override
    protected void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actions, (MySQLView) command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actionList, (MySQLView) command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(List<DBEPersistAction> actions, MySQLView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        decl.append("CREATE OR REPLACE VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getAdditionalInfo().getDefinition()); //$NON-NLS-1$
        final MySQLView.CheckOption checkOption = view.getAdditionalInfo().getCheckOption();
        if (checkOption != null && checkOption != MySQLView.CheckOption.NONE) {
            decl.append(lineSeparator).append("WITH ").append(checkOption.getDefinitionName()).append(" CHECK OPTION"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        actions.add(new SQLDatabasePersistAction("Create view", decl.toString()));
    }

}

