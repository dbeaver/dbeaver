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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Package;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;

/**
 * DB2PackageManager
 */
public class DB2PackageManager extends JDBCObjectEditor<DB2Package, DB2Schema> {

   @Override
   public DBSObjectCache<? extends DBSObject, DB2Package> getObjectsCache(DB2Package object) {
      return object.getSchema().getPackageCache();
   }

   @Override
   protected DB2Package createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                             DBECommandContext context,
                                             DB2Schema parent,
                                             Object copyFrom) {
      CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(),
                                                         parent.getDataSource(),
                                                         DB2Messages.edit_db2_package_manager_dialog_title);
      if (dialog.open() != IDialogConstants.OK_ID) {
         return null;
      }
      // return new DB2Package(
      // parent,
      // dialog.getEntityName());
      return null;
   }

   @Override
   protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand) {
      return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
   }

   @Override
   protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand) {
      final DB2Package object = objectDeleteCommand.getObject();
      return new IDatabasePersistAction[] { new AbstractDatabasePersistAction("Drop package",
                                                                              "DROP PACKAGE " + object.getFullQualifiedName()) //$NON-NLS-1$
      };
   }

   @Override
   protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand) {
      return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
   }

   @Override
   public long getMakerOptions() {
      return FEATURE_EDITOR_ON_CREATE;
   }

   private IDatabasePersistAction[] createOrReplaceProcedureQuery(DB2Package pack) {
      List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
      // String header = DB2Utils.normalizeSourceName(pack, false);
      // if (!CommonUtils.isEmpty(header)) {
      // actions.add(
      // new AbstractDatabasePersistAction(
      // "Create package header",
      //                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
      // }
      // String body = DB2Utils.normalizeSourceName(pack, true);
      // if (!CommonUtils.isEmpty(body)) {
      // actions.add(
      // new AbstractDatabasePersistAction(
      // "Create package body",
      //                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
      // } else {
      // actions.add(
      // new AbstractDatabasePersistAction(
      // "Drop package header",
      //                    "DROP PACKAGE BODY " + pack.getFullQualifiedName(), IDatabasePersistAction.ActionType.OPTIONAL) //$NON-NLS-1$
      // );
      // }
      // DB2Utils.addSchemaChangeActions(actions, pack);
      return actions.toArray(new IDatabasePersistAction[actions.size()]);
   }

}
