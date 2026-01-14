/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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

public class YashanDBTableForeignKey extends YashanDBTableConstraintBase implements DBSTableForeignKey {

	private static final Log log = Log.getLog(YashanDBTableForeignKey.class);

	private YashanDBTableConstraint referencedKey;
	private DBSForeignKeyModifyRule deleteRule;

	public YashanDBTableForeignKey(@NotNull YashanDBTableBase table, @Nullable String name,
			@Nullable YashanDBObjectStatus status, @Nullable YashanDBTableConstraint referencedKey,
			@NotNull DBSForeignKeyModifyRule deleteRule) {
		super(table, name, DBSEntityConstraintType.FOREIGN_KEY, status, false);
		this.referencedKey = referencedKey;
		this.deleteRule = deleteRule;
	}

	public YashanDBTableForeignKey(DBRProgressMonitor monitor, YashanDBTable table, ResultSet dbResult)
			throws DBException {
		super(table, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), DBSEntityConstraintType.FOREIGN_KEY,
				CommonUtils.notNull(CommonUtils.valueOf(YashanDBObjectStatus.class,
						JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS")), YashanDBObjectStatus.ENABLED),
				true);

		String refName = JDBCUtils.safeGetString(dbResult, "R_CONSTRAINT_NAME");
		String refOwnerName = JDBCUtils.safeGetString(dbResult, "R_OWNER");
		String refTableName = JDBCUtils.safeGetString(dbResult, "R_TABLE_NAME");
		YashanDBTableBase refTable = YashanDBTableBase.findTable(monitor, table.getDataSource(), refOwnerName,
				refTableName);
		if (refTable == null) {
			log.warn("Referenced table '" + DBUtils.getSimpleQualifiedName(refOwnerName, refTableName) + "' not found");
		} else {
			referencedKey = refTable.getConstraint(monitor, refName);
			if (referencedKey == null) {
				log.warn("Referenced constraint '" + refName + "' not found in table '"
						+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
				referencedKey = new YashanDBTableConstraint(refTable, "refName", DBSEntityConstraintType.UNIQUE_KEY,
						null, YashanDBObjectStatus.ERROR);
			}
		}

		String deleteRuleName = JDBCUtils.safeGetString(dbResult, "DELETE_RULE");
		if (CommonUtils.isEmpty(deleteRuleName)) {
			this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
		} else {
			switch (deleteRuleName) {
			case "CASCADE":
				this.deleteRule = DBSForeignKeyModifyRule.CASCADE;
				break;
			case "SET NULL":
				this.deleteRule = DBSForeignKeyModifyRule.SET_NULL;
				break;
			case "NO ACTION":
			default:
				this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
				break;
			}
		}
	}

	@Property(viewable = true, order = 3)
	public YashanDBTableBase getReferencedTable() {
		return referencedKey == null ? null : referencedKey.getTable();
	}

	@Nullable
	@Override
	@Property(id = "reference", viewable = true, order = 4)
	public YashanDBTableConstraint getReferencedConstraint() {
		return referencedKey;
	}

	public void setReferencedConstraint(YashanDBTableConstraint referencedKey) {
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

	@NotNull
	@Override
	public DBSForeignKeyModifyRule getUpdateRule() {
		return DBSForeignKeyModifyRule.NO_ACTION;
	}

	@Override
	public YashanDBTableBase getAssociatedEntity() {
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
