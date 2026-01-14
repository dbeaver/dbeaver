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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;

public class YashanDBTableConstraintColumn extends AbstractTableConstraintColumn {

	private AbstractTableConstraint<YashanDBTableBase, YashanDBTableConstraintColumn> constraint;
	private YashanDBTableColumn tableColumn;
	private int ordinalPosition;

	public YashanDBTableConstraintColumn(
			AbstractTableConstraint<YashanDBTableBase, YashanDBTableConstraintColumn> constraint,
			YashanDBTableColumn tableColumn, int ordinalPosition) {
		this.constraint = constraint;
		this.tableColumn = tableColumn;
		this.ordinalPosition = ordinalPosition;
	}

	@NotNull
	@Override
	public String getName() {
		return tableColumn.getName();
	}

	@NotNull
	@Override
	@Property(id = "name", viewable = true, order = 1)
	public YashanDBTableColumn getAttribute() {
		return tableColumn;
	}

	@Override
	@Property(viewable = false, order = 2)
	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	@Nullable
	@Override
	public String getDescription() {
		return tableColumn.getDescription();
	}

	@Override
	public AbstractTableConstraint<YashanDBTableBase, YashanDBTableConstraintColumn> getParentObject() {
		return constraint;
	}

	@NotNull
	@Override
	public YashanDBDataSource getDataSource() {
		return constraint.getTable().getDataSource();
	}
}
