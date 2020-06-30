/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2019 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

public class ExasolTablePartitionColumn extends AbstractTableIndexColumn {

	
	private ExasolTable table;
	private int ordinalPosition;
	private ExasolTableColumn tableColumn;
	private static final Log log = Log.getLog(ExasolTablePartitionColumn.class);
	
	
	public ExasolTablePartitionColumn(ExasolTable table, ExasolTableColumn tableColumn, int ordinalPosition)
	{
		this.table = table;
		this.tableColumn = tableColumn;
		this.ordinalPosition = ordinalPosition;
	}
	
	public ExasolTablePartitionColumn(ExasolTable table) {
		this.table = table;
	}
	
	public ExasolTable getTable() {
		return table;
	}
	
	@Override
	@Property(viewable = false)
	public DBSTableIndex getIndex() {
		return null;
	}
	
	@Override
    @Property(viewable = true, updatable=true, editable=true, order = 2)
	public int getOrdinalPosition() {
		return ordinalPosition;
	}
	
	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	@Override
    @Property(viewable = false)
	public boolean isAscending() {
		return false;
	}

	@Override
    @Property(viewable = true, editable = true, order = 1, listProvider = TableColumListProvider.class)
	public ExasolTableColumn getTableColumn() {
		return tableColumn;
	}
	
	public void setTableColumn(ExasolTableColumn tableColumn) {
		if (tableColumn == null) {
			throw new IllegalArgumentException();
		}
		this.tableColumn = tableColumn;
	}

	@Override
    @Property(viewable = false)
	public String getDescription() {
		return tableColumn.getDescription();
	}

	@Override
	public ExasolTable getParentObject() {
		return this.table;
	}

	@Override
	public ExasolDataSource getDataSource() {
		return table.getDataSource();
	}

	@Override
    @Property(viewable = false)
	public String getName() {
		return tableColumn.getName();
	}
	
	public static class TableColumListProvider implements IPropertyValueListProvider<ExasolTablePartitionColumn> {

		@Override
		public boolean allowCustomValue() {
			return true;
		}

		@Override
		public Object[] getPossibleValues(ExasolTablePartitionColumn object) {
			try {
				return ((ExasolTable) object.getTable()).getAvailableColumns(new VoidProgressMonitor()).toArray();
			} catch (DBException e) {
				log.error("Failed to get list of available columns",e);
				return new Object[0];
			}
		}
		
	}

}
