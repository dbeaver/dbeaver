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

package org.jkiss.dbeaver.ext.db2.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DDLFormat;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;
import org.jkiss.utils.CommonUtils;

/**
 * DB2ObjectDDLEditor
 */
public class DB2ObjectDDLEditor extends SQLEditorNested<DB2Table> {

   private DB2DDLFormat ddlFormat = DB2DDLFormat.FULL;

   public DB2ObjectDDLEditor() {
   }

   @Override
   public void init(IEditorSite site, IEditorInput input) throws PartInitException {
      super.init(site, input);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
      String ddlFormatString = getEditorInput().getDatabaseObject().getDataSource().getContainer().getPreferenceStore()
               .getString(DB2Constants.PREF_KEY_DDL_FORMAT);
      if (!CommonUtils.isEmpty(ddlFormatString)) {
         try {
            ddlFormat = DB2DDLFormat.valueOf(ddlFormatString);
         } catch (IllegalArgumentException e) {
            log.error(e);
         }
      }
      return ((DB2Table) getEditorInput().getDatabaseObject()).getDDL(monitor, ddlFormat);
   }

   @Override
   protected void setSourceText(String sourceText) {
   }

   @Override
   protected void contributeEditorCommands(ToolBarManager toolBarManager) {
      super.contributeEditorCommands(toolBarManager);

      toolBarManager.add(new Separator());
      toolBarManager.add(new ControlContribution("DDLFormat") {
         @Override
         protected Control createControl(Composite parent) {
            final Combo ddlFormatCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
            ddlFormatCombo.setToolTipText("DDL Format");
            for (DB2DDLFormat format : DB2DDLFormat.values()) {
               ddlFormatCombo.add(format.getTitle());
               if (format == ddlFormat) {
                  ddlFormatCombo.select(ddlFormatCombo.getItemCount() - 1);
               }
            }
            ddlFormatCombo.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
                  for (DB2DDLFormat format : DB2DDLFormat.values()) {
                     if (format.ordinal() == ddlFormatCombo.getSelectionIndex()) {
                        ddlFormat = format;
                        getEditorInput().getDatabaseObject().getDataSource().getContainer().getPreferenceStore()
                                 .setValue(DB2Constants.PREF_KEY_DDL_FORMAT, ddlFormat.name());
                        refreshPart(this, true);
                        break;
                     }
                  }
               }
            });
            return ddlFormatCombo;
         }
      });
   }

}