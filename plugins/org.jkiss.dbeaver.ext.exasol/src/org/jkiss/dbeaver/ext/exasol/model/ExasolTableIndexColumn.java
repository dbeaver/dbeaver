/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

public class ExasolTableIndexColumn extends AbstractTableIndexColumn {
	
	private ExasolTableIndex index;
	private int ordinalPosition;
	private ExasolTableColumn tableColumn;

	public ExasolTableIndexColumn(
			ExasolTableIndex index,
			ExasolTableColumn tableColumn,
			int ordinalPosition
			) 
	{
		this.index = index;
		this.ordinalPosition = ordinalPosition;
		this.tableColumn = tableColumn;
	}
	

	@Override
	public ExasolTableIndex getIndex() {
		return this.index;
	}

	@Override
    @Property(viewable = false, order = 2)
	public int getOrdinalPosition() {
		return this.ordinalPosition;
	}

	@Override
	public boolean isAscending() {
		return true;
	}

	@Override
	public ExasolTableIndex getParentObject() {
		return index;
	}

	@Override
	public DBPDataSource getDataSource() {
		return index.getDataSource();
	}

	@Override
	public String getName() {
		return tableColumn.getName();
	}
	
    @Nullable
    @Override
    @Property(id = "name", viewable = true, order = 1)
    public ExasolTableColumn getTableColumn()
    {
        return tableColumn;
    }
	

	@Override
	public String getDescription() {
		// Index has no description in Exasol
		return "";
	}
	
}
