/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreView;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreViewManager
 */
public class PostgreViewManager extends SQLObjectEditor<PostgreTableBase, PostgreSchema> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().tableCache;
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
        PostgreTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(((PostgreView) object).getSource())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Override
    protected PostgreView createDatabaseObject(DBECommandContext context, PostgreSchema parent, Object copyFrom)
    {
        PostgreView newCatalog = new PostgreView(parent);
        newCatalog.setName("NewView"); //$NON-NLS-1$
        return newCatalog;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery((PostgreView) command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery((PostgreView) command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceViewQuery(PostgreView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        decl.append("CREATE OR REPLACE VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getSource()); //$NON-NLS-1$
/*
        final PostgreView.CheckOption checkOption = view.getAdditionalInfo().getCheckOption();
        if (checkOption != null && checkOption != PostgreView.CheckOption.NONE) {
            decl.append(lineSeparator).append("WITH ").append(checkOption.getDefinitionName()).append(" CHECK OPTION"); //$NON-NLS-1$ //$NON-NLS-2$
        }
*/
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Create view", decl.toString())
        };
    }

}

