package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DM Table Index Column
 * 
 * @author caosw
 *
 */
public class DmTableIndexColumn extends AbstractTableIndexColumn {

	private DmTableIndex index;
	private DmTableColumn tableColumn;
	private int ordinalPosition;
	private boolean ascending;
	private String columnExpression;

	public DmTableIndexColumn(DmTableIndex index, DmTableColumn tableColumn, int ordinalPosition, boolean ascending,
			String columnExpression) {
		this.index = index;
		this.tableColumn = tableColumn;
		this.ordinalPosition = ordinalPosition;
		this.ascending = ascending;
		this.columnExpression = columnExpression;
	}

	DmTableIndexColumn(DmTableIndex toIndex, DmTableIndexColumn source) {
		this.index = toIndex;
		this.tableColumn = source.tableColumn;
		this.ordinalPosition = source.ordinalPosition;
		this.ascending = source.ascending;
		this.columnExpression = source.columnExpression;
	}

	@NotNull
	@Override
	public DmTableIndex getIndex() {
		return index;
	}

	// @Property(name = "Name", viewable = true, order = 1)
	@NotNull
	@Override
	public String getName() {
		return tableColumn.getName();
	}

	@Nullable
	@Override
	@Property(id = "name", viewable = true, order = 1)
	public DmTableColumn getTableColumn() {
		return tableColumn;
	}

	@Override
	@Property(viewable = false, order = 2)
	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	@Override
	@Property(viewable = true, order = 3)
	public boolean isAscending() {
		return ascending;
	}

	@Property(viewable = true, order = 4)
	public String getColumnExpression() {
		return columnExpression;
	}

	@Nullable
	@Override
	public String getDescription() {
		return tableColumn.getDescription();
	}

	@Override
	public DmTableIndex getParentObject() {
		return index;
	}

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return index.getDataSource();
	}

}
