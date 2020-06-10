package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.DBPObject;

public class SQLToolStatisticsSimple extends SQLToolStatistics{
    boolean isError;

    private String errorMessage;

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    protected SQLToolStatisticsSimple(DBPObject object, boolean isError) {
        super(object);
        if(isError){
            errorMessage = "ERROR";
        }
    }
}
