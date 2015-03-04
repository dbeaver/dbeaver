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
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
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
    protected OracleView createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleView newView = new OracleView(parent, "NewView"); //$NON-NLS-1$
        return newView;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command);
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command);
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceViewQuery(DBECommandComposite<OracleView, PropertyHandler> command)
    {
        final OracleView view = command.getObject();
        boolean hasComment = command.getProperty("comment") != null;
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>(2);
        if (!hasComment || command.getProperties().size() > 1) {
            StringBuilder decl = new StringBuilder(200);
            final String lineSeparator = ContentUtils.getDefaultLineSeparator();
            decl.append("CREATE OR REPLACE VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
                .append("AS ").append(view.getAdditionalInfo().getText()); //$NON-NLS-1$
            actions.add(new SQLDatabasePersistAction("Create view", decl.toString()));
        }
        if (hasComment) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + view.getFullQualifiedName() +
                    " IS '" + view.getComment() + "'"));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}

