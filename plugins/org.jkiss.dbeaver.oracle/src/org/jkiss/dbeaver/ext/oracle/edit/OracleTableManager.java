/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.editors.OracleTableDDLSection;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

/**
 * Oracle table manager
 */
public class OracleTableManager extends JDBCTableManager<OracleTable, OracleSchema> implements DBEObjectRenamer<OracleTable>, DBEObjectTabProvider<OracleTable> {

    private static final Class<?>[] CHILD_TYPES = {
        OracleTableColumn.class,
        OracleConstraint.class,
        OracleForeignKey.class,
        OracleIndex.class
    };

    @Override
    protected OracleTable createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        return new OracleTable(
            parent,
            DBObjectNameCaseTransformer.transformName(parent, "NewTable"));
    }

    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        StringBuilder query = new StringBuilder("ALTER TABLE ");
        query.append(command.getObject().getFullQualifiedName()).append(" ");
        appendTableModifiers(command.getObject(), command, query);

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(query.toString())
        };
    }

    @Override
    protected void appendTableModifiers(OracleTable table, NestedObjectCommand tableProps, StringBuilder ddl)
    {
    }

    protected IDatabasePersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Rename table",
                "RENAME TABLE " + command.getObject().getFullQualifiedName() +
                    " TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName()))
        };
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    public void renameObject(DBECommandContext commandContext, OracleTable object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleTable object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "table.ddl",
                "DDL",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "DDL") {
                    public ISection getSectionClass()
                    {
                        return new OracleTableDDLSection(activeEditor);
                    }
                })
        };
    }

}
