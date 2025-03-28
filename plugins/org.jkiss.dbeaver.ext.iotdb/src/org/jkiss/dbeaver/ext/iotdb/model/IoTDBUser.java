package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class IoTDBUser extends IoTDBAbstractUser {

    public IoTDBUser(IoTDBDataSource dataSource,
                     String userName,
                     DBRProgressMonitor monitor) throws DBException {
        super(dataSource, userName, monitor);
    }
}
