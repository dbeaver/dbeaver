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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLViewManager
 */
public class MySQLViewManager extends SQLObjectEditor<MySQLTableBase, MySQLCatalog> {

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
    protected MySQLView createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, MySQLCatalog parent, Object copyFrom)
    {
        MySQLView newCatalog = new MySQLView(parent);
        newCatalog.setName("NewView"); //$NON-NLS-1$
        return newCatalog;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery((MySQLView) command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery((MySQLView) command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceViewQuery(MySQLView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = ContentUtils.getDefaultLineSeparator();
        decl.append("CREATE OR REPLACE VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getAdditionalInfo().getDefinition()); //$NON-NLS-1$
        final MySQLView.CheckOption checkOption = view.getAdditionalInfo().getCheckOption();
        if (checkOption != null && checkOption != MySQLView.CheckOption.NONE) {
            decl.append(lineSeparator).append("WITH ").append(checkOption.getDefinitionName()).append(" CHECK OPTION"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Create view", decl.toString())
        };
    }

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseEditor activeEditor, final MySQLView object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "view.definition", //$NON-NLS-1$
                MySQLMessages.edit_view_manager_definition,
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", MySQLMessages.edit_view_manager_definition) { //$NON-NLS-1$
                    public ISection getSectionClass()
                    {
                        return new MySQLViewDefinitionSection(activeEditor);
                    }
                })
        };
    }
*/
}

