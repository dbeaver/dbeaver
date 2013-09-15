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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2View;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DB2ViewManager
 */
public class DB2ViewManager extends JDBCObjectEditor<DB2View, DB2Schema> {
   private static final String SQL_DROP_VIEW = "DROP VIEW %s";

   @Override
   public long getMakerOptions() {
      return FEATURE_EDITOR_ON_CREATE;
   }

   @Override
   protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
      if (CommonUtils.isEmpty(command.getObject().getName())) {
         throw new DBException("View name cannot be empty");
      }
   }

   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public DBSObjectCache<? extends DBSObject, DB2View> getObjectsCache(DB2View object) {
      return (DBSObjectCache) object.getSchema().getViewCache();
   }

   @Override
   protected DB2View createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                          DBECommandContext context,
                                          DB2Schema parent,
                                          Object copyFrom) {
      //        DB2View newView = new DB2View(parent, "NewView"); //$NON-NLS-1$
      // return newView;
      return null;
   }

   @Override
   protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command) {
      return createOrReplaceViewQuery(command);
   }

   @Override
   protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command) {
      return createOrReplaceViewQuery(command);
   }

   @Override
   protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command) {
      String sql = String.format(SQL_DROP_VIEW, command.getObject().getFullQualifiedName());
      IDatabasePersistAction action = new AbstractDatabasePersistAction("Drop view", sql);
      return new IDatabasePersistAction[] { action };
   }

   private IDatabasePersistAction[] createOrReplaceViewQuery(DBECommandComposite<DB2View, PropertyHandler> command) {
      final DB2View view = command.getObject();
      boolean hasComment = command.getProperty("comment") != null;
      List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>(2);
      if (!hasComment || command.getProperties().size() > 1) {
         StringBuilder decl = new StringBuilder(200);
         final String lineSeparator = ContentUtils.getDefaultLineSeparator();
         //            decl.append("CREATE OR REPLACE VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
         //                .append("AS ").append(view.getAdditionalInfo().getText()); //$NON-NLS-1$
         actions.add(new AbstractDatabasePersistAction("Create view", decl.toString()));
      }
      if (hasComment) {
         // actions.add(new AbstractDatabasePersistAction(
         // "Comment table",
         // "COMMENT ON TABLE " + view.getFullQualifiedName() +
         // " IS '" + view.getRemarks() + "'"));
      }
      return actions.toArray(new IDatabasePersistAction[actions.size()]);
   }

}
