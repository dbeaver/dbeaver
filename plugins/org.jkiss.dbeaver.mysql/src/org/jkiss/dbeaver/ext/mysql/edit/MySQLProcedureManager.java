/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.editors.MySQLProcedureBodySection;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLProcedureManager
 */
public class MySQLProcedureManager extends JDBCObjectEditor<MySQLProcedure, MySQLCatalog> implements DBEObjectTabProvider<MySQLProcedure> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getBody())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    @Override
    protected MySQLProcedure createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, MySQLCatalog parent, Object copyFrom)
    {
        CreateProcedureDialog dialog = new CreateProcedureDialog(workbenchWindow.getShell(), parent.getDataSource());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        MySQLProcedure newProcedure = new MySQLProcedure(parent);
        newProcedure.setProcedureType(dialog.getProcedureType());
        newProcedure.setName(dialog.getProcedureName());
        return newProcedure;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceProcedureQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceProcedureQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop procedure", "DROP PROCEDURE " + command.getObject().getFullQualifiedName())
        };
    }

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(MySQLProcedure procedure)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop procedure", "DROP " + procedure.getProcedureType() + " IF EXISTS " + procedure.getFullQualifiedName()),
            new AbstractDatabasePersistAction("Create procedure", "CREATE " + procedure.getClientBody())
        };
    }

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final MySQLProcedure object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "procedure.body",
                "Body",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Body") {
                    public ISection getSectionClass()
                    {
                        return new MySQLProcedureBodySection(activeEditor);
                    }
                })
        };
    }


}

