package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.model.DBPObject;

public class DB2PlanConfig implements DBPObject {

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
}
