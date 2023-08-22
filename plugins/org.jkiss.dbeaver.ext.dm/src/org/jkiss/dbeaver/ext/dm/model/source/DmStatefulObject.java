package org.jkiss.dbeaver.ext.dm.model.source;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DmStatefulObject
 * @author caosw
 *
 */
public interface DmStatefulObject extends DBSObject, DBPStatefulObject {

	@NotNull
    @Override
	DmDataSource getDataSource();
	
	@NotNull
	DmSchema getSchema();
}
