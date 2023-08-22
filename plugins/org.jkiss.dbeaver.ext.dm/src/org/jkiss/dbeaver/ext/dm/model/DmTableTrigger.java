package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DM Table Trigger
 * 
 * @author caosw
 *
 */
public class DmTableTrigger extends DmTrigger<DmTableBase> {

	private static final Log log = Log.getLog(DmTableTrigger.class);

	private DmSchema ownerSchema;

	public DmTableTrigger(DmTableBase table, String name) {
		super(table, name);
		ownerSchema = table.getSchema();
	}

	public DmTableTrigger(DmTableBase table, ResultSet dbResult) {
		super(table, dbResult);
		String ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "OWNER");
		if (ownerName != null) {
			this.ownerSchema = table.getDataSource().schemaCache.getCachedObject(ownerName);
			if (this.ownerSchema == null) {
				log.warn("Trigger owner schema '" + ownerName + "' not found");
			}
		}
		if (this.ownerSchema == null) {
			this.ownerSchema = table.getSchema();
		}
	}

	@Override
	@Property(viewable = true, order = 4)
	public DmTableBase getTable() {
		return parent;
	}

	@Override
	public DmSchema getSchema() {
		return this.ownerSchema;
	}

	@Association
	public Collection<DmTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException {
		return parent.triggerCache.getChildren(monitor, parent, this);
	}
}
