/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DataSource user.
 * May be editor, view or selection element
 */
public interface IDataSourceUser {

    /**
     * Underlying datasource
     * @return data source object.
     */
    DBSDataSourceContainer getDataSourceContainer();

    /**
     * Underlying datasource
     * @return data source object.
     */
    DBPDataSource getDataSource();

    /**
     * Currently used session. If datasource user do not obtains dedicated session then returns null.
     * @return session object or null.
     * @throws DBException on error
     */
    DBCSession getSession() throws DBException;

}
