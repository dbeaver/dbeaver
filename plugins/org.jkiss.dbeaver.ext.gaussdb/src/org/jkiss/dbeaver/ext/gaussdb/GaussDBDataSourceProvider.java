package org.jkiss.dbeaver.ext.gaussdb;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBDataSourceProvider extends JDBCDataSourceProvider {

   @Override
   public long getFeatures() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public DBPDataSource openDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
      // TODO Auto-generated method stub
      return null;
   }

}