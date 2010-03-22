package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.meta.DBMModel;

/**
 * DBPApplication
 */
public interface DBPApplication
{
    DBMModel getMetaModel();

    DBPRegistry getDataSourceRegistry();

}
