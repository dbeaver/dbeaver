package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.DBPObject;

public class SQLToolStatisticsSimple extends SQLToolStatistics{
    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

    protected SQLToolStatisticsSimple(DBPObject object, boolean isError) {
        super(object);
        if(isError){
            message = "ERROR";
        }
        else{
            message = "OK";
        }
    }
}
