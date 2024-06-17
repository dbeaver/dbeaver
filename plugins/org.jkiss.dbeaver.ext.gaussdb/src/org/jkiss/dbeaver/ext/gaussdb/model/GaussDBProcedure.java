package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBProcedure extends PostgreProcedure {

   public GaussDBProcedure(PostgreSchema schema) {
      super(schema);
      // TODO Auto-generated constructor stub
   }

   public GaussDBProcedure(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
      super(monitor, schema, dbResult);
      // TODO Auto-generated constructor stub
   }

}