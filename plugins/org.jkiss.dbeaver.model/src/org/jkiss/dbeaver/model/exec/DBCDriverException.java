package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;

public class DBCDriverException extends DBCException {
    private static final long serialVersionUID = 1L;
    private String driverClassName;
    private String driverFullName;
    private DBPDriver driver;

    public DBCDriverException(String message) {
        super(message);
    }

    public DBCDriverException(String message, String driverClassName, String driverFullName, DBPDriver driver,  Throwable e) {
        super(message, e);
        this.driverClassName = driverClassName;
        this.driverFullName = driverFullName;
        this.driver =  driver;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDriverFullName() {
        return driverFullName;
    }
    
    public DBPDriver getDriver() {
        return driver;
    }
    
    @Override
    public String getMessage() {
        return "Error instantiation of driver name: '"
            + getDriverFullName()
            + "',  class: '"
            + getDriverClassName()
            + "'"
            + "\nCause: "
            + "\n\t 1. Driver libraries (.jars) is not supplied in product"
            + "\n\t 2. Driver libraries (.jars) not found in path"
            + "\n\t 3. Driver configuration is not valid";
    }
}
