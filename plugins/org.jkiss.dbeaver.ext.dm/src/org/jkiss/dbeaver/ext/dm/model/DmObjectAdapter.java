package org.jkiss.dbeaver.ext.dm.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DmObjectAdapter implements IAdapterFactory {

	public DmObjectAdapter() {

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
	public Class[] getAdapterList() {
		return new Class[] { DmSourceObject.class, DmProcedurePackaged.class, DBPScriptObjectExt.class };
	}
}
