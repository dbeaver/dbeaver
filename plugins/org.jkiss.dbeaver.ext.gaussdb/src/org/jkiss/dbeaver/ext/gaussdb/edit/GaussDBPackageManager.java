package org.jkiss.dbeaver.ext.gaussdb.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDataSource;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBPackage;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class GaussDBPackageManager extends SQLObjectEditor<GaussDBPackage, GaussDBDatabase> implements
                                   DBEObjectRenamer<GaussDBPackage> {

   @Override
   public long getMakerOptions(DBPDataSource dataSource) {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public DBSObjectCache<? extends DBSObject, GaussDBPackage> getObjectsCache(GaussDBPackage object) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void renameObject(DBECommandContext commandContext,
                            GaussDBPackage object,
                            Map<String, Object> options,
                            String newName) throws DBException {
      // TODO Auto-generated method stub

   }

   @Override
   protected GaussDBPackage createDatabaseObject(DBRProgressMonitor monitor,
                                                 DBECommandContext context,
                                                 Object container,
                                                 Object copyFrom,
                                                 Map<String, Object> options) throws DBException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected void addObjectCreateActions(DBRProgressMonitor monitor,
                                         DBCExecutionContext executionContext,
                                         List<DBEPersistAction> actions,
                                         SQLObjectEditor<GaussDBPackage, GaussDBDatabase>.ObjectCreateCommand command,
                                         Map<String, Object> options) throws DBException {
      // TODO Auto-generated method stub

   }

   @Override
   protected void addObjectDeleteActions(DBRProgressMonitor monitor,
                                         DBCExecutionContext executionContext,
                                         List<DBEPersistAction> actions,
                                         SQLObjectEditor<GaussDBPackage, GaussDBDatabase>.ObjectDeleteCommand command,
                                         Map<String, Object> options) throws DBException {
      // TODO Auto-generated method stub

   }

}