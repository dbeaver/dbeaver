package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;

/**
 * DBSStructureContainerActive
 */
public interface DBSStructureContainerActive
{
    boolean supportsActiveChildChange();

    DBSObject getActiveChild() throws DBException;

    void setActiveChild(DBSObject child) throws DBException;

}