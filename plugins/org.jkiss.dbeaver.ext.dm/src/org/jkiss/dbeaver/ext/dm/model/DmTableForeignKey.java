package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import javax.swing.plaf.metal.DefaultMetalTheme;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.utils.CommonUtils;

public class DmTableForeignKey extends DmTableConstraintBase implements DBSTableForeignKey {

	private static final Log log = Log.getLog(DmTableForeignKey.class);

	private DmTableConstraint referencedKey;
	private DBSForeignKeyModifyRule deleteRule;

	public DmTableForeignKey(@NotNull DmTableBase DmTable, @Nullable String name, @Nullable DmObjectStatus status,
			@Nullable DmTableConstraint referencedKey, @NotNull DBSForeignKeyModifyRule deleteRule) {
		super(DmTable, name, DBSEntityConstraintType.FOREIGN_KEY, status, false);
		this.referencedKey = referencedKey;
		this.deleteRule = deleteRule;
	}

	public DmTableForeignKey(DBRProgressMonitor monitor, DmTable table, ResultSet dbResult) throws DBException {
		super(table, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), DBSEntityConstraintType.FOREIGN_KEY,
				CommonUtils.notNull(
						CommonUtils.valueOf(DmObjectStatus.class, "Y".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"))?"ENABLED":JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS")),
						DmObjectStatus.ENABLED),
				true);

		String refName = JDBCUtils.safeGetString(dbResult, "R_CONSTRAINT_NAME");
		String refOwnerName = JDBCUtils.safeGetString(dbResult, "R_OWNER");
		String refTableName = JDBCUtils.safeGetString(dbResult, "R_TABLE_NAME");
		DmTableBase refTable = DmTableBase.findTable(monitor, table.getDataSource(), refOwnerName, refTableName);
		if (refTable == null) {
			log.warn("Referenced table '" + DBUtils.getSimpleQualifiedName(refOwnerName, refTableName) + "' not found");
		} else {
			referencedKey = refTable.getConstraint(monitor, refName);
			if (referencedKey == null) {
				log.warn("Referenced constraint '" + refName + "' not found in table '"
						+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
				referencedKey = new DmTableConstraint(refTable, "refName", DBSEntityConstraintType.UNIQUE_KEY, null,
						DmObjectStatus.ERROR);
			}
		}

		String deleteRuleName = JDBCUtils.safeGetString(dbResult, "DELETE_RULE");
		this.deleteRule = "CASCADE".equals(deleteRuleName) ? DBSForeignKeyModifyRule.CASCADE
				: DBSForeignKeyModifyRule.NO_ACTION;
	}

	@Property(viewable = true, order = 3)
	public DmTableBase getReferencedTable() {
		return referencedKey == null ? null : referencedKey.getTable();
	}

	@Nullable
	@Override
	@Property(id = "reference", viewable = true, order = 4)
	public DmTableConstraint getReferencedConstraint() {
		return referencedKey;
	}

	public void setReferencedConstraint(DmTableConstraint referencedKey) {
		this.referencedKey = referencedKey;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
	public DBSForeignKeyModifyRule getDeleteRule() {
		return deleteRule;
	}

	public void setDeleteRule(DBSForeignKeyModifyRule deleteRule) {
		this.deleteRule = deleteRule;
	}

	// Update rule is not supported by Dm
	@NotNull
	@Override
	public DBSForeignKeyModifyRule getUpdateRule() {
		return DBSForeignKeyModifyRule.NO_ACTION;
	}

	@Override
	public DmTableBase getAssociatedEntity() {
		return getReferencedTable();
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
	}

	public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {

		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(JDBCTableForeignKey foreignKey) {
			return new DBSForeignKeyModifyRule[] { DBSForeignKeyModifyRule.NO_ACTION, DBSForeignKeyModifyRule.CASCADE,
					DBSForeignKeyModifyRule.RESTRICT, DBSForeignKeyModifyRule.SET_NULL,
					DBSForeignKeyModifyRule.SET_DEFAULT };
		}
	}
}
