package org.jkiss.dbeaver.ext.ui;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DataSource user.
 * May be editor, view or selection element
 */
public interface IDataSourceUser {

    DBPDataSource getDataSource();

}
