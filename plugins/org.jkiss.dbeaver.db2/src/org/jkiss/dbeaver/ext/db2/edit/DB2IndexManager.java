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

import java.util.Collections;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 index manager
 */
public class DB2IndexManager extends JDBCIndexManager<DB2Index, DB2Table> {

   @Override
   public DBSObjectCache<? extends DBSObject, DB2Index> getObjectsCache(DB2Index object) {
      return object.getParentObject().getSchema().getIndexCache();
   }

   @Override
   protected DB2Index createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2Table parent, Object from) {
      EditIndexDialog editDialog = new EditIndexDialog(workbenchWindow.getShell(),
                                                       DB2Messages.edit_db2_index_manager_dialog_title,
                                                       parent,
                                                       Collections.singletonList(DBSIndexType.OTHER));
      if (editDialog.open() != IDialogConstants.OK_ID) {
         return null;
      }

      StringBuilder idxName = new StringBuilder(64);
      idxName.append(CommonUtils.escapeIdentifier(parent.getName())).append("_") //$NON-NLS-1$
               .append(CommonUtils.escapeIdentifier(editDialog.getSelectedColumns().iterator().next().getName())).append("_IDX"); //$NON-NLS-1$
      // final DB2TableIndex index = new DB2TableIndex(parent.getSchema(),parent, DBObjectNameCaseTransformer.transformName(
      // (DBPDataSource) parent.getDataSource(), idxName.toString()), false, editDialog.getIndexType());
      // int colIndex = 1;
      // for (DBSEntityAttribute tableColumn : editDialog.getSelectedColumns()) {
      // index.addColumn(new DB2TableIndexColumn(index, (DB2TableColumn) tableColumn, colIndex++, true));
      // }
      // return index;
      return null;
   }

   @Override
   protected String getDropIndexPattern(DB2Index index) {
      return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
   }

}
