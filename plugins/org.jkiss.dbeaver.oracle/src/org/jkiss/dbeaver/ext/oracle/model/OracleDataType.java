package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Oracle data type
 */
public class OracleDataType implements DBSObject, DBSDataType{



    public boolean isPersisted()
    {
        return true;
    }

    public int getTypeNumber()
    {
        return java.sql.Types.OTHER;
    }

    public DBSDataKind getDataKind()
    {
        return DBSDataKind.UNKNOWN;
    }

    public boolean isUnsigned()
    {
        return false;
    }

    public boolean isSearchable()
    {
        return false;
    }

    public int getPrecision()
    {
        return 0;
    }

    public int getMinScale()
    {
        return 0;
    }

    public int getMaxScale()
    {
        return 0;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return schema;
    }

    public DBPDataSource getDataSource()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
