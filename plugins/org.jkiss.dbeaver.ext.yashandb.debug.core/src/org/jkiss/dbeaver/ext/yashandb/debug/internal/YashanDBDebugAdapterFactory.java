
package org.jkiss.dbeaver.ext.yashandb.debug.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGControllerFactory;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.ext.yashandb.YashanDBDataSourceProvider;
import org.jkiss.dbeaver.ext.yashandb.debug.internal.impl.YashanDBDebugControllerFactory;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

public class YashanDBDebugAdapterFactory implements IAdapterFactory {



    private static final Class<?>[] CLASSES = new Class[] { DBGController.class, DBGResolver.class };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGControllerFactory.class) {
            if (adaptableObject instanceof DBPDataSourceContainer) {
                DBPDataSourceContainer ds = (DBPDataSourceContainer) adaptableObject;
                if (ds.getDriver().getDataSourceProvider() instanceof YashanDBDataSourceProvider) {
                    return adapterType.cast(new YashanDBDebugControllerFactory());
                }
            }
        } else if (adapterType == DBGResolver.class) {
            if (adaptableObject instanceof DBPDataSourceContainer) {
                DBPDataSourceContainer ds = (DBPDataSourceContainer) adaptableObject;
                if (ds.getDriver().getProviderId().equals(YashanDBDataSourceProvider.PROVIDER_ID)) {
                    return adapterType.cast(new YashanDBResolver(ds));
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
