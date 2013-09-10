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

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DB2 table column manager
 */
public class DB2TableColumnManager extends JDBCTableColumnManager<DB2TableColumn, DB2TableBase> {

   @Override
   public DBSObjectCache<? extends DBSObject, DB2TableColumn> getObjectsCache(DB2TableColumn object) {

      // return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
      return null;
   }

   @Override
   public StringBuilder getNestedDeclaration(DB2TableBase owner, DBECommandComposite<DB2TableColumn, PropertyHandler> command) {
      StringBuilder decl = super.getNestedDeclaration(owner, command);
      final DB2TableColumn column = command.getObject();
      // if (!CommonUtils.isEmpty(column.getComment())) {
      //            decl.append(" COMMENT '").append(column.getComment()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
      // }
      return decl;
   }

   @Override
   protected DB2TableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                                 DBECommandContext context,
                                                 DB2TableBase parent,
                                                 Object copyFrom) {
      DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2"); //$NON-NLS-1$

      // final DB2TableColumn column = new DB2TableColumn(parent);
      // column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
      // // column.setType((DB2DataType) columnType);
      //      column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
      // column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
      // column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
      // column.setOrdinalPosition(-1);
      // return column;
      return null;
   }

   @Override
   protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command) {
      final DB2TableColumn column = command.getObject();
      List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>(2);
      boolean hasComment = command.getProperty("comment") != null;
      if (!hasComment || command.getProperties().size() > 1) {
         actions.add(new AbstractDatabasePersistAction("Modify column", "ALTER TABLE " + column.getTable().getFullQualifiedName() + //$NON-NLS-1$
                  " MODIFY " + getNestedDeclaration(column.getTable(), command))); //$NON-NLS-1$
      }
      if (hasComment) {
         actions.add(new AbstractDatabasePersistAction("Comment column", "COMMENT ON COLUMN "
                  + column.getTable().getFullQualifiedName() + "." + DBUtils.getQuotedIdentifier(column) + " IS '"
                  + column.getComment() + "'"));
      }
      return actions.toArray(new IDatabasePersistAction[actions.size()]);
   }
}
