/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.actions;

import java.sql.SQLException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

public class DB2SchemaToolsHandler extends AbstractHandler {

   private static final String DROP = "org.jkiss.dbeaver.ext.db2.schema.drop";
   private static final String COPY = "org.jkiss.dbeaver.ext.db2.schema.copy";

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);

      DB2Schema sourceSchema = RuntimeUtils.getObjectAdapter(selection.getFirstElement(), DB2Schema.class);

      if (sourceSchema != null) {
         if (event.getCommand().getId().equals(DROP)) {

            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

            NewSchemaDialog dialog = new NewSchemaDialog(activeShell);
            if (dialog.open() != IDialogConstants.OK_ID) {
               return null;
            }

            try {
               Boolean res = DB2Utils.callAdminDropSchema(VoidProgressMonitor.INSTANCE, sourceSchema.getDataSource(),
                                                          sourceSchema.getName(), dialog.getErrorSchemaName(),
                                                          dialog.getErrorTableName());
               // TOOD DF: refresh worspace
               sourceSchema.refreshObject(VoidProgressMonitor.INSTANCE);
               // DBNModel.getInstance().refreshNodeContent(sourceSchema, this, DBNEvent.NodeChange.REFRESH);

            } catch (SQLException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            } catch (DBException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

         } else {
            UIUtils.showMessageBox(HandlerUtil.getActiveShell(event), "Copy schema", "Create schema '" + sourceSchema.getName()
                     + "' copy", SWT.ICON_INFORMATION);

         }
      }

      return null;
   }

   static class NewSchemaDialog extends Dialog {

      private String errorSchemaName;
      private String errorTableName;

      public String getErrorSchemaName() {
         return errorSchemaName;
      }

      public String getErrorTableName() {
         return errorTableName;
      }

      // Dialog managment
      private Text errorSchmaNameText;
      private Text errorTableNameText;

      public NewSchemaDialog(Shell parentShell) {
         super(parentShell);
      }

      @Override
      protected boolean isResizable() {
         return true;
      }

      @Override
      protected Control createDialogArea(Composite parent) {
         getShell().setText("Name for Error Table?");
         Control container = super.createDialogArea(parent);
         Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
         composite.setLayoutData(new GridData(GridData.FILL_BOTH));

         errorSchmaNameText = UIUtils.createLabelText(composite, "Schema", null);
         errorSchmaNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

         errorTableNameText = UIUtils.createLabelText(composite, "Name", null);
         errorTableNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

         return parent;
      }

      @Override
      protected void okPressed() {
         this.errorSchemaName = errorSchmaNameText.getText().trim().toUpperCase();
         this.errorTableName = errorTableNameText.getText().trim().toUpperCase();
         super.okPressed();
      }
   }
}