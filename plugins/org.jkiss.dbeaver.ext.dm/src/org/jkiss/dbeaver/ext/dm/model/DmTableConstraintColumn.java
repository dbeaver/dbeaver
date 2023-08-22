package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;
/**
 * DM TableConstraint Column
 * @author caosw
 *
 */
public class DmTableConstraintColumn extends AbstractTableConstraintColumn {

	private AbstractTableConstraint<DmTableBase> constraint;
	private DmTableColumn tableColumn;
	private int ordinalPosition;

	public DmTableConstraintColumn(AbstractTableConstraint<DmTableBase> constraint, DmTableColumn tableColumn,
			int ordinalPosition) {
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
	public DmTableColumn getAttribute() {
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

	public AbstractTableConstraint<DmTableBase> getParentObject() {
		return constraint;
	}
	
	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return constraint.getTable().getDataSource();
	}
}
