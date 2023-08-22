package org.jkiss.dbeaver.ext.yashandb.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/5 11:52
 */
public class YashanDBObjectAdapter implements IAdapterFactory {

    public YashanDBObjectAdapter() {
    }

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (DBSObject.class.isAssignableFrom(adapterType)) {
            DBSObject dbObject = null;
            if (adaptableObject instanceof DBNDatabaseNode) {
                dbObject = ((DBNDatabaseNode) adaptableObject).getObject();
            }
            if (dbObject != null && adapterType.isAssignableFrom(dbObject.getClass())) {
                return adapterType.cast(dbObject);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[] {  YashanDBSchedulerJob.class };
    }
}

