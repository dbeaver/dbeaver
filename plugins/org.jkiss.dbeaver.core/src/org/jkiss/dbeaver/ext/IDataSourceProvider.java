/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DataSource provider.
 * May be editor, view or selection element
 */
public interface IDataSourceProvider {

    /**
     * Underlying datasource
     * @return data source object.
     */
    DBPDataSource getDataSource();

}
