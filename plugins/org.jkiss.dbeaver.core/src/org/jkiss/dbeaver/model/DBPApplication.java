/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.navigator.DBNModel;

/**
 * DBPApplication
 */
public interface DBPApplication
{
    DBNModel getNavigatorModel();

    DBPRegistry getDataSourceRegistry();

}
