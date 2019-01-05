/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2019 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

public class ExasolTablePartitionColumn extends AbstractTableIndexColumn {

	
	private ExasolTable table;
	private int ordinalPosition;
	private ExasolTableColumn tableColumn;
	
	
	public ExasolTablePartitionColumn(ExasolTable table, ExasolTableColumn tableColumn, int ordinalPosition)
	{
		this.table = table;
		this.tableColumn = tableColumn;
		this.ordinalPosition = ordinalPosition;
	}
	
	@Override
	public DBSTableIndex getIndex() {
		return null;
	}

	@Override
    @Property(viewable = true, order = 3)
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
    @Property(viewable = true, order = 2)
	public ExasolTableColumn getTableColumn() {
		return tableColumn;
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
    @Property(viewable = true, order = 1)
	public String getName() {
		return tableColumn.getName();
	}

}
