package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;

public class GaussDBSchema extends PostgreSchema {

   public GaussDBSchema(PostgreDatabase database, String name, PostgreRole owner) {
      super(database, name, owner);
      // TODO Auto-generated constructor stub
   }

   public GaussDBSchema(PostgreDatabase database, String name, ResultSet dbResult) throws SQLException {
      super(database, name, dbResult);
      // TODO Auto-generated constructor stub
   }

}