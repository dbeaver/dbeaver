/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DataSource user.
 * May be editor, view or selection element
 */
public interface IDataSourceEditor {

    /**
     * Underlying datasource
     * @return data source object.
     */
    DBPDataSource getDataSource();

}
