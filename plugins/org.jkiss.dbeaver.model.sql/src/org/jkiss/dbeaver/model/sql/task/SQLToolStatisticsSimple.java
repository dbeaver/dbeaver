package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.meta.Property;

public class SQLToolStatisticsSimple extends SQLToolStatistics{
    private String statusMessage;

    @Property(viewable = true, editable = true, updatable = true)
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    protected SQLToolStatisticsSimple(DBPObject object, boolean isError) {
        super(object);
        if(isError){
            statusMessage = "ERROR";
        }
        else{
            statusMessage = "OK";
        }
    }
}
