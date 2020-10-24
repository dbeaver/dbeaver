package org.jkiss.dbeaver.ext.hana.model;

import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class HANAView extends GenericView {

    public HANAView(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Association
    public List<HANADependency> getDependencies(DBRProgressMonitor monitor) throws DBException {
        return HANADependency.readDependencies(monitor, this);
    }
}
