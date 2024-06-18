package org.jkiss.dbeaver.ext.gaussdb.model;

import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreScriptObject;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class GaussDBPackage implements PostgreObject, PostgreScriptObject, DBPSystemInfoObject {

   public GaussDBPackage(GaussDBSchema schema, DBRProgressMonitor monitor, String string) {
        // TODO Auto-generated constructor stub
    }

public GaussDBPackage(JDBCSession session, GaussDBSchema owner, JDBCResultSet dbResult) {
    // TODO Auto-generated constructor stub
}

@Override
   public DBSObject getParentObject() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getName() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getDescription() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isPersisted() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public long getObjectId() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setObjectDefinitionText(String sourceText) throws DBException {
      // TODO Auto-generated method stub

   }

   @Override
   public PostgreDataSource getDataSource() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public PostgreDatabase getDatabase() {
      // TODO Auto-generated method stub
      return null;
   }

public GaussDBSchema getSchema() {
    // TODO Auto-generated method stub
    return null;
}

public String getExtendedDefinitionText() {
    // TODO Auto-generated method stub
    return null;
}

public String getObjectDefinitionText() {
    // TODO Auto-generated method stub
    return null;
}

}