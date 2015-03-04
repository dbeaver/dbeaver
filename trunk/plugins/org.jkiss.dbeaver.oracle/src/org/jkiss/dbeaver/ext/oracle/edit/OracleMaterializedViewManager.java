/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

/**
 * OracleMaterializedViewManager
 */
public class OracleMaterializedViewManager extends SQLObjectEditor<OracleMaterializedView, OracleSchema> {

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
            throw new DBException("View name cannot be empty"); //$NON-NLS-1$
        }
        if (CommonUtils.isEmpty(command.getObject().getSourceDeclaration(null))) {
            throw new DBException("View definition cannot be empty"); //$NON-NLS-1$
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleMaterializedView> getObjectsCache(OracleMaterializedView object)
    {
        return object.getSchema().mviewCache;
    }

    @Override
    protected OracleMaterializedView createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleMaterializedView newView = new OracleMaterializedView(parent, "NewView"); //$NON-NLS-1$
        return newView;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceViewQuery(OracleMaterializedView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = ContentUtils.getDefaultLineSeparator();
        decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getSourceDeclaration(null)); //$NON-NLS-1$
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + view.getFullQualifiedName()), //$NON-NLS-2$
            new SQLDatabasePersistAction("Create view", decl.toString())
        };
    }

}

