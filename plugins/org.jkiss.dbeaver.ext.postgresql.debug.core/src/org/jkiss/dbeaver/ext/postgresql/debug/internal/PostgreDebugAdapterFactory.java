package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugController;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.connection.DBPDriver;

public class PostgreDebugAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBGController.class, DBGResolver.class };

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGController.class) {
            if (adaptableObject instanceof DBPDataSourceContainer) {
                DBPDataSourceContainer sourceContainer = (DBPDataSourceContainer) adaptableObject;
                DBPDriver driver = sourceContainer.getDriver();
                if (driver == null) {
                    return null;
                }
                DBPDataSourceProvider dataSourceProvider = driver.getDataSourceProvider();
                if (dataSourceProvider instanceof PostgreDataSourceProvider) {
                    PostgreDebugController postgreDebugController = new PostgreDebugController(sourceContainer);
                    return (T) postgreDebugController;
                }

            }
        } else if (adapterType == DBGResolver.class) {
            if (adaptableObject instanceof DBPDataSourceContainer) {
                DBPDataSourceContainer sourceContainer = (DBPDataSourceContainer) adaptableObject;
                DBPDataSource dataSource = sourceContainer.getDataSource();
                if (dataSource instanceof PostgreDataSource) {
                    PostgreDataSource postgeDataSource = (PostgreDataSource) dataSource;
                    return (T) new PostgreResolver(postgeDataSource);
                }
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
