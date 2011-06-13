/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.HashMap;
import java.util.Map;

public class OracleDataSourceProvider extends JDBCDataSourceProvider {

    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<String, String>();

        // Program name
        connectionsProps.put("v$session.program", "DBeaver " + Platform.getProduct().getDefiningBundle().getVersion());
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public OracleDataSourceProvider()
    {
    }

    @Override
    protected String getConnectionPropertyDefaultValue(String name, String value) {
        String ovrValue = connectionsProps.get(name);
        return ovrValue != null ? ovrValue : super.getConnectionPropertyDefaultValue(name, value);
    }

    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        return new OracleDataSource(container);
    }

}
