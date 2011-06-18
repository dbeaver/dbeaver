/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.editors.MySQLViewDefinitionSection;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.*;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * MySQLViewManager
 */
public class MySQLViewManager extends JDBCObjectEditor<MySQLView, MySQLCatalog> implements DBEObjectTabProvider<MySQLView> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getAdditionalInfo().getDefinition())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Override
    protected MySQLView createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, MySQLCatalog parent, Object copyFrom)
    {
        MySQLView newCatalog = new MySQLView(parent);
        newCatalog.setName("NewView");
        return newCatalog;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop view", "DROP VIEW " + command.getObject().getFullQualifiedName())
        };
    }

    private IDatabasePersistAction[] createOrReplaceViewQuery(MySQLView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = ContentUtils.getDefaultLineSeparator();
        decl.append("CREATE OR REPLACE VIEW ").append(view.getFullQualifiedName()).append(lineSeparator)
            .append("AS ").append(view.getAdditionalInfo().getDefinition());
        final MySQLView.CheckOption checkOption = view.getAdditionalInfo().getCheckOption();
        if (checkOption != null && checkOption != MySQLView.CheckOption.NONE) {
            decl.append(lineSeparator).append("WITH ").append(checkOption.getDefinitionName()).append(" CHECK OPTION");
        }
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Create view", decl.toString())
        };
    }

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final MySQLView object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "view.definition",
                "Definition",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Definition") {
                    public ISection getSectionClass()
                    {
                        return new MySQLViewDefinitionSection(activeEditor);
                    }
                })
        };
    }
}

