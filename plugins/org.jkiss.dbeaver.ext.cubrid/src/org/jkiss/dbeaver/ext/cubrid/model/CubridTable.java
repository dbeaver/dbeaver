/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

public class CubridTable extends GenericTable {

	private static final Log log = Log.getLog(CubridTable.class);
	private String ddl;
	private CubridUser owner;
	private CubridObjectContainer container;
	private boolean isSystemTable;

	public CubridTable(CubridObjectContainer container, @Nullable String tableName, @Nullable String tableType,
			@Nullable JDBCResultSet dbResult) {
		super(container, tableName, tableType, dbResult);
		this.container = container;
		String owner_name;

		if (dbResult != null) {
			owner_name = JDBCUtils.safeGetString(dbResult, CubridConstants.OWNER_NAME);
			isSystemTable = JDBCUtils.safeGetString(dbResult, CubridConstants.IS_SYSTEM_CLASS).equals("YES");
		} else {
			owner_name = getDataSource().getContainer().getConnectionConfiguration().getUserName().toUpperCase();
			isSystemTable = false;
		}

		for (CubridUser cbOwner : getUsers()) {
			if (cbOwner.getName().equals(owner_name)) {
				this.owner = cbOwner;
				break;
			}
		}
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
		return true;
	}

	boolean isHideVirtualModel() {
		return true;
	}

	public boolean isPhysicalTable() {
		return !isView();
	}

	@Override
	public boolean isSystem() {
		return isSystemTable;
	}

	public CubridObjectContainer getContainer() {
		return this.container;
	}

	public Collection<? extends CubridUser> getUsers() {
		try {
			return container.getDataSource().getCubridUsers(null);
		} catch (DBException e) {
			log.error("Cannot get user.");
		}
		return null;
	}

	public Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.getContainer().getCubridIndexCache().getObjects(monitor, getContainer(), this);
	}

	@Nullable
	@Property(viewable = true, editable = true, updatable = true, listProvider = OwnerListProvider.class, order = 2)
	public CubridUser getOwner() {
		return owner;
	}

	public void setOwner(CubridUser owner) {
		this.owner = owner;
	}

	public String getUniqueName() {
		return this.owner.getName() + "." + this.getName();
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.refreshObject(monitor);
		return this.getContainer().getCubridTableCache().refreshObject(monitor, container, this);
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		if (isSystemTable) {
			return DBUtils.getFullQualifiedName(getDataSource(), this);
		} else {
			return DBUtils.getFullQualifiedName(getDataSource(), getOwner(), this);
		}
	}

	public static class OwnerListProvider implements IPropertyValueListProvider<CubridTable> {

		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(CubridTable object) {
			return object.getUsers().toArray();
		}
	}

}
