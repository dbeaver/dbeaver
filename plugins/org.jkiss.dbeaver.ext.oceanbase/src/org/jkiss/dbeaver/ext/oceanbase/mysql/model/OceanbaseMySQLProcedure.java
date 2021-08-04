package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.sql.ResultSet;
import java.util.Collection;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedureParameter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class OceanbaseMySQLProcedure extends MySQLProcedure{
	private OceanbaseMySQLCatalog container = (OceanbaseMySQLCatalog)getContainer();

	public OceanbaseMySQLProcedure(MySQLCatalog catalog) {
		super(catalog);
	}
	
	public OceanbaseMySQLProcedure(MySQLCatalog catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }
	
	@Override
    public Collection<MySQLProcedureParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        return container.oceanbaseProceduresCache.getChildren(monitor, container, this);
    }
	
	@Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return container.oceanbaseProceduresCache.refreshObject(monitor, container, this);
    }
	
}
