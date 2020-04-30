package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DB2PlanConfig implements DBSObject {

    private String tablespace;

    private String sessionUserSchema;

    public String getTablespace() {
        return tablespace;
    }

    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    public String getSessionUserSchema() {
        return sessionUserSchema;
    }

    public void setSessionUserSchema(String sessionUserSchema) {
        this.sessionUserSchema = sessionUserSchema;
    }

    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Override
    public DBPDataSource getDataSource() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return false;
    }
}
