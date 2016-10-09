/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.editors;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * @author Karl
 *
 */
public interface ExasolStatefulObject extends DBSObject, DBPStatefulObject {
	@NotNull
	@Override
	ExasolDataSource getDataSource();
	ExasolSchema getSchema();

}
