package org.jkiss.dbeaver.ext.yashandb.debug.ui.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.ui.DBGEditorAdvisor;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

public class YashanDBDebugUIAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[]{DBGEditorAdvisor.class};

    private DBGEditorAdvisor debugEditorAdvisor = new YashanDBSourceEditorAdvisor();

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGEditorAdvisor.class) {
            if (adaptableObject instanceof DBPDataSourceContainer) {
                DBPDataSourceContainer sourceContainer = (DBPDataSourceContainer) adaptableObject;
                DBPDataSource dataSource = sourceContainer.getDataSource();
                if (dataSource instanceof YashanDBDataSource) {
                    return adapterType.cast(debugEditorAdvisor);
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
