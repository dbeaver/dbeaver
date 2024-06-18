package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBFunction extends GaussDBProcedure {

    public GaussDBFunction(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
    }

    public GaussDBFunction(PostgreSchema schema) {
        super(schema);
    }

}