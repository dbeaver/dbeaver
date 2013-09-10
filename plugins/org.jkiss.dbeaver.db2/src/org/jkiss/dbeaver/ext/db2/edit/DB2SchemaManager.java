/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.edit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2User;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DB2SchemaManager
 */
public class DB2SchemaManager extends JDBCObjectEditor<DB2Schema, DB2DataSource> implements DBEObjectRenamer<DB2Schema> {

   @Override
   public long getMakerOptions() {
      return FEATURE_SAVE_IMMEDIATELY;
   }

   @Override
   public DBSObjectCache<? extends DBSObject, DB2Schema> getObjectsCache(DB2Schema object) {
      return object.getDataSource().getSchemaCache();
   }

   @Override
   protected DB2Schema createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                            DBECommandContext context,
                                            DB2DataSource parent,
                                            Object copyFrom) {
      NewUserDialog dialog = new NewUserDialog(workbenchWindow.getShell(), parent);
      if (dialog.open() != IDialogConstants.OK_ID) {
         return null;
      }
      DB2Schema newSchema = new DB2Schema(parent, null);
      newSchema.setName(dialog.getUser().getName());

      return newSchema;
   }

   @Override
   protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command) {
      StringBuilder sb = new StringBuilder(64);
      sb.append("CREATE SCHEMA ");
      sb.append(command.getObject().getName());
      return new IDatabasePersistAction[] { new AbstractDatabasePersistAction("Create schema", sb.toString()) };
   }

   @Override
   protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command) {
      StringBuilder sb = new StringBuilder(64);
      sb.append("DROP SCHEMA ");
      sb.append(command.getObject().getName());
      sb.append(" RESTRICT");

      IDatabasePersistAction[] actions = new IDatabasePersistAction[1];
      actions[0] = new AbstractDatabasePersistAction("Drop schema", sb.toString()); //$NON-NLS-2$
      return actions;
   }

   @Override
   public void renameObject(DBECommandContext commandContext, DB2Schema schema, String newName) throws DBException {
      throw new DBException("Direct schema rename is not yet implemented in DB2. You should use export/import functions for that.");
   }

   static class NewUserDialog extends Dialog {

      private DB2User user;
      private Text    nameText;
      private Text    passwordText;

      public NewUserDialog(Shell parentShell, DB2DataSource dataSource) {
         super(parentShell);
         // this.user = new DB2User(dataSource);
      }

      public DB2User getUser() {
         return user;
      }

      @Override
      protected boolean isResizable() {
         return true;
      }

      @Override
      protected Control createDialogArea(Composite parent) {
         getShell().setText("Set schema/user properties");
         Control container = super.createDialogArea(parent);
         Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
         composite.setLayoutData(new GridData(GridData.FILL_BOTH));

         nameText = UIUtils.createLabelText(composite, "Schema/User Name", null);
         nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
         passwordText = UIUtils.createLabelText(composite, "User Password", null, SWT.BORDER | SWT.PASSWORD);
         passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

         return parent;
      }

      @Override
      protected void okPressed() {
         // user.setName(DBObjectNameCaseTransformer.transformName(user, nameText.getText()));
         // user.setPassword(passwordText.getText());
         super.okPressed();
      }

   }

}
