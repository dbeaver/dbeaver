package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;

/**
 * Meta object
 */
public interface DBSObject extends DBPObject
{
    /**
     * Object name
     *
     * @return object name
     */
    String getName();

    /**
     * Object description
     *
     * @return object description or null
     */
    String getDescription();

    /**
     * Parent object
     *
     * @return parent object or null
     */
	DBSObject getParentObject();

    /**
     * Datasource which this object belongs
     * @return datasource reference
     */
    DBPDataSource getDataSource();

    /**
     * Refresh object's (and all ща шеэы children) state
     * @return true if object refreshed and false if parent object have to be refreshed
     * to perform requested operation
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean refreshObject()
        throws DBException;

}
