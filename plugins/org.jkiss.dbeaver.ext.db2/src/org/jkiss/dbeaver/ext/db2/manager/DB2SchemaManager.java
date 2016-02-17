/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DB2 Schema Manager
 * 
 * @author Denis Forveille
 */
public class DB2SchemaManager extends SQLObjectEditor<DB2Schema, DB2DataSource> {

    private static final String SQL_CREATE_SCHEMA = "CREATE SCHEMA %s";
    private static final String SQL_DROP_SCHEMA = "DROP SCHEMA %s RESTRICT";

    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2Schema> getObjectsCache(DB2Schema object)
    {
        return object.getDataSource().getSchemaCache();
    }

    @Override
    protected DB2Schema createDatabaseObject(DBECommandContext context, DB2DataSource parent,
                                             Object copyFrom)
    {
        NewSchemaDialog dialog = new NewSchemaDialog(DBeaverUI.getActiveWorkbenchShell());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        String schemaName = dialog.getSchemaName();
        if (schemaName.length() == 0) {
            return null;
        }
        DB2Schema newSchema = new DB2Schema(parent, schemaName);

        return newSchema;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        SQLDatabasePersistAction action = new SQLDatabasePersistAction("Create schema", String.format(SQL_CREATE_SCHEMA,
            DBUtils.getQuotedIdentifier(command.getObject())));
        return new DBEPersistAction[] { action };
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        String schemaName = command.getObject().getName();
        DBEPersistAction action = new SQLDatabasePersistAction("Drop schema (SQL)", String.format(SQL_DROP_SCHEMA,
            DBUtils.getQuotedIdentifier(command.getObject())));
        return new DBEPersistAction[] { action };
    }

    // --------
    // Dialog
    // --------

    private static class NewSchemaDialog extends Dialog {

        private String schemaName;

        public String getSchemaName()
        {
            return schemaName;
        }

        // Dialog managment
        private Text nameText;

        public NewSchemaDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("New Schema Name");
            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Schema Name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed()
        {
            this.schemaName = nameText.getText().trim().toUpperCase();
            super.okPressed();
        }

    }

}
