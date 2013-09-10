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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DB2 table manager
 */
public class DB2TableManager extends JDBCTableManager<DB2Table, DB2Schema> implements DBEObjectRenamer<DB2Table> {

   private static final Class<?>[] CHILD_TYPES = { DB2TableColumn.class, DB2TableUniqueKey.class, DB2TableForeignKey.class,
            DB2Index.class                    };

   @Override
   public DBSObjectCache<? extends DBSObject, DB2Table> getObjectsCache(DB2Table object) {
      return (DBSObjectCache) object.getSchema().getTableCache();
   }

   @Override
   protected DB2Table createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                           DBECommandContext context,
                                           DB2Schema parent,
                                           Object copyFrom) {
      return null;
      //return new DB2Table(parent, DBObjectNameCaseTransformer.transformName(parent, "NewTable")); //$NON-NLS-1$
   }

   @Override
   protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command) {
      final DB2Table table = command.getObject();
      boolean hasComment = command.getProperty("comment") != null;
      List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>(2);
      if (!hasComment || command.getProperties().size() > 1) {
         StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
         query.append(command.getObject().getFullQualifiedName()).append(" "); //$NON-NLS-1$
         appendTableModifiers(command.getObject(), command, query);
         actions.add(new AbstractDatabasePersistAction(query.toString()));
      }
      if (hasComment) {
         // actions.add(new AbstractDatabasePersistAction("Comment table", "COMMENT ON TABLE " + table.getFullQualifiedName()
         // + " IS '" + table.getRemarks() + "'"));
      }

      return actions.toArray(new IDatabasePersistAction[actions.size()]);
   }

   @Override
   protected void appendTableModifiers(DB2Table table, NestedObjectCommand tableProps, StringBuilder ddl) {
   }

   @Override
   protected IDatabasePersistAction[] makeObjectRenameActions(ObjectRenameCommand command) {
      StringBuilder sb = new StringBuilder(256);
      sb.append("RENAME TABLE ");
      sb.append(command.getObject().getName());
      sb.append(" TO ");
      sb.append(command.getNewName());

      IDatabasePersistAction[] actions = new IDatabasePersistAction[1];
      actions[0] = new AbstractDatabasePersistAction("Rename table", sb.toString()); //$NON-NLS-1$
      return actions;
   }

   @Override
   public Class<?>[] getChildTypes() {
      return CHILD_TYPES;
   }

   @Override
   public void renameObject(DBECommandContext commandContext, DB2Table object, String newName) throws DBException {
      processObjectRename(commandContext, object, newName);
   }

}
