package org.jkiss.dbeaver.ext.oracle.model.dict;

/**
* Connection role
*/
public enum OracleConnectionRole
{
    NORMAL("Normal"),
    SYSDBA("SYSDBA"),
    SYSOPER("SYSOPER");
    private final String title;

    OracleConnectionRole(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
