package org.jkiss.dbeaver.debug;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;

public interface DBGControllerRegistry {

    DBGController createController(DBPDataSourceContainer dataSource) throws DBGException;

}
