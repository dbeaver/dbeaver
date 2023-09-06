
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGControllerFactory;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.Map;

public class YashanDBDebugControllerFactory implements DBGControllerFactory {


    @Override
    public DBGController createController(DBPDataSourceContainer dataSource, Map<String, Object> context) throws DBGException {
        return new YashanDBDebugController(dataSource, context);
    }
}
