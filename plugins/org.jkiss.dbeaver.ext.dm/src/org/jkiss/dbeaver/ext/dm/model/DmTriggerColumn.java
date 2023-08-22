package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DM Trigger Column
 * 
 * @author caosw
 *
 */
public class DmTriggerColumn extends AbstractTriggerColumn {

	private static final Log log = Log.getLog(DmTrigger.class);

	private DmTrigger trigger;
	private String name;
	private DmTableColumn tableColumn;
	private boolean columnList;

	public DmTriggerColumn(DBRProgressMonitor monitor, DmTrigger trigger, DmTableColumn tableColumn, ResultSet dbResult)
			throws DBException {
		this.trigger = trigger;
		this.tableColumn = tableColumn;
		this.name = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
		this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
	}

	public DmTriggerColumn(DmTrigger trigger, DmTriggerColumn source) {
		this.trigger = trigger;
		this.tableColumn = source.tableColumn;
		this.columnList = source.columnList;
	}

	@Override
	public DmTrigger getTrigger() {
		return trigger;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Override
	@Property(viewable = true, order = 2)
	public DmTableColumn getTableColumn() {
		return tableColumn;
	}

	@Override
	public int getOrdinalPosition() {
		return 0;
	}

	@Nullable
	@Override
	public String getDescription() {
		return tableColumn.getDescription();
	}

	@Override
	public DmTrigger getParentObject() {
		return trigger;
	}

	@Override
	@NotNull
	public DmDataSource getDataSource() {
		return trigger.getDataSource();
	}

	@Override
	public String toString() {
		return getName();
	}
}
