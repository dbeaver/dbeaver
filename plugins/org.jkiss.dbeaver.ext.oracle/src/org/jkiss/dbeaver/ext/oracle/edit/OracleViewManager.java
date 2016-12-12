/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * OracleViewManager
 */
public class OracleViewManager extends SQLObjectEditor<OracleView, OracleSchema> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getAdditionalInfo().getText())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleView> getObjectsCache(OracleView object)
    {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    @Override
    protected OracleView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleView newView = new OracleView(parent, "NEW_VIEW"); //$NON-NLS-1$
        newView.getAdditionalInfo().setText("CREATE OR REPLACE VIEW " + newView.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\nSELECT");
        return newView;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        createOrReplaceViewQuery(actions, command);
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        createOrReplaceViewQuery(actionList, command);
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(List<DBEPersistAction> actions, DBECommandComposite<OracleView, PropertyHandler> command)
    {
        final OracleView view = command.getObject();
        boolean hasComment = command.getProperty("comment") != null;
        if (!hasComment || command.getProperties().size() > 1) {
            actions.add(new SQLDatabasePersistAction("Create view", view.getAdditionalInfo().getText()));
        }
        if (hasComment) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS '" + view.getComment() + "'"));
        }
    }

}

