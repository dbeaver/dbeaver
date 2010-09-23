/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DataSource provider editor.
 * May be editor, view or selection element
 */
public interface IDataSourceContainerProvider {

    /**
     * Underlying datasource container
     * @return data source object.
     */
    DBSDataSourceContainer getDataSourceContainer();

}