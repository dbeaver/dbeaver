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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class YashanDBTriggerColumn extends AbstractTriggerColumn {

	private YashanDBTrigger trigger;
	private String name;
	private YashanDBTableColumn tableColumn;
	private boolean columnList;

	public YashanDBTriggerColumn(DBRProgressMonitor monitor, YashanDBTrigger trigger, YashanDBTableColumn tableColumn,
			ResultSet dbResult) throws DBException {
		this.trigger = trigger;
		this.tableColumn = tableColumn;
		this.name = JDBCUtils.safeGetString(dbResult, "TRIGGER_COLUMN_NAME");
		this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
	}

	YashanDBTriggerColumn(YashanDBTrigger trigger, YashanDBTriggerColumn source) {
		this.trigger = trigger;
		this.tableColumn = source.tableColumn;
		this.columnList = source.columnList;
	}

	@Override
	public YashanDBTrigger getTrigger() {
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
	public YashanDBTableColumn getTableColumn() {
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
	public YashanDBTrigger getParentObject() {
		return trigger;
	}

	@NotNull
	@Override
	public YashanDBDataSource getDataSource() {
		return trigger.getDataSource();
	}

	@Override
	public String toString() {
		return getName();
	}
}
