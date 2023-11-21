package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

public class CubridTable extends CubridTableBase implements DBPScriptObjectExt2 {

	private String ddl;

	public CubridTable(
		CubridObjectContainer container,
		@Nullable String tableName,
		@Nullable String tableType,
		@Nullable JDBCResultSet dbResult) {
		super(container, tableName, tableType, dbResult);
	}

	@Override
	public boolean isView() {
		return false;
	}

	@Override
	public String getDDL() {
		return ddl;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
			ddl = null;
		}
		if (!isPersisted()) {
			return DBStructUtils.generateTableDDL(monitor, this, options, false);
		}
		if (ddl == null || !isCacheDDL()) {
			ddl = getDataSource().getMetaModel().getTableDDL(monitor, this, options);
		}
		return ddl;
	}

	protected boolean isCacheDDL() {
		return true;
	}

	@Override
	public boolean supportsObjectDefinitionOption(String option) {
		if (OPTION_DDL_ONLY_FOREIGN_KEYS.equals(option) || OPTION_DDL_SKIP_FOREIGN_KEYS.equals(option)) {
			// DDL split supported only by base meta model
			return !isPersisted() || getDataSource().getMetaModel().supportsTableDDLSplit(this);
		}
		return false;
	}

	boolean isHideVirtualModel(){
		return true;
	}

}
