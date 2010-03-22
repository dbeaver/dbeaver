package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.dbc.DBCStateType;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.List;

/**
 * DBPDataSourceUser
 */
public interface DBPDataSourceUser
{
    /**
     * Checks this users needs active connection.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean needsConnection();

}