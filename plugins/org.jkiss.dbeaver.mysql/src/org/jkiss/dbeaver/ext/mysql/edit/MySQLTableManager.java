/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.editors.MySQLTableDDLSection;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

/**
 * MySQL table manager
 */
public class MySQLTableManager extends JDBCTableManager<MySQLTable, MySQLCatalog> implements DBEObjectRenamer<MySQLTable>, DBEObjectTabProvider<MySQLTable> {

    private static final Class<?>[] CHILD_TYPES = {
        MySQLTableColumn.class,
        MySQLConstraint.class,
        MySQLForeignKey.class,
        MySQLIndex.class
    };

    @Override
    protected MySQLTable createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, MySQLCatalog parent, Object copyFrom)
    {
        final MySQLTable table = new MySQLTable(parent);
        table.setName(DBObjectNameCaseTransformer.transformName(parent, "NewTable")); //$NON-NLS-1$
        try {
            final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(VoidProgressMonitor.INSTANCE);
            additionalInfo.setEngine(parent.getDataSource().getDefaultEngine());
            additionalInfo.setCharset(parent.getDefaultCharset());
            additionalInfo.setCollation(parent.getDefaultCollation());
        } catch (DBCException e) {
            // Never be here
            log.error(e);
        }

        return table;
    }

    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
        query.append(command.getObject().getFullQualifiedName()).append(" "); //$NON-NLS-1$
        appendTableModifiers(command.getObject(), command, query);

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(query.toString())
        };
    }

    @Override
    protected void appendTableModifiers(MySQLTable table, NestedObjectCommand tableProps, StringBuilder ddl)
    {
        try {
            final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(VoidProgressMonitor.INSTANCE);
            if ((!table.isPersisted() || tableProps.getProperty("engine") != null) && additionalInfo.getEngine() != null) { //$NON-NLS-1$
                ddl.append("\nENGINE=").append(additionalInfo.getEngine().getName()); //$NON-NLS-1$
            }
            if ((!table.isPersisted() || tableProps.getProperty("charset") != null) && additionalInfo.getCharset() != null) { //$NON-NLS-1$
                ddl.append("\nDEFAULT CHARSET=").append(additionalInfo.getCharset().getName()); //$NON-NLS-1$
            }
            if ((!table.isPersisted() || tableProps.getProperty("collation") != null) && additionalInfo.getCollation() != null) { //$NON-NLS-1$
                ddl.append("\nCOLLATE=").append(additionalInfo.getCollation().getName()); //$NON-NLS-1$
            }
            if ((!table.isPersisted() || tableProps.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) && table.getDescription() != null) {
                ddl.append("\nCOMMENT='").append(table.getDescription().replace('\'', '"')).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if ((!table.isPersisted() || tableProps.getProperty("autoIncrement") != null) && additionalInfo.getAutoIncrement() > 0) { //$NON-NLS-1$
                ddl.append("\nAUTO_INCREMENT=").append(additionalInfo.getAutoIncrement()); //$NON-NLS-1$
            }
        } catch (DBCException e) {
            log.error(e);
        }
    }

    protected IDatabasePersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                MySQLMessages.edit_table_manager_action_rename_table,
                "RENAME TABLE " + command.getObject().getFullQualifiedName() + //$NON-NLS-1$
                    " TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        };
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    public void renameObject(DBECommandContext commandContext, MySQLTable object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseEditor activeEditor, final MySQLTable object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "table.ddl", //$NON-NLS-1$
                "DDL", //$NON-NLS-1$
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "DDL") { //$NON-NLS-1$ //$NON-NLS-2$
                    public ISection getSectionClass()
                    {
                        return new MySQLTableDDLSection(activeEditor);
                    }
                })
        };
    }

}
