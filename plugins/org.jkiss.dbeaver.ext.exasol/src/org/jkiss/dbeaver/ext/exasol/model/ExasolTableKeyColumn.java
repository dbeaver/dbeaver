package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;


/**
 * @author Karl Griesser
 *
 */
public class ExasolTableKeyColumn extends AbstractTableConstraintColumn {
	
	private AbstractTableConstraint<ExasolTable> constraint;
	private ExasolTableColumn tableColumn;
	private Integer ordinalPosition;

	
    // -----------------
    // Constructors
    // -----------------
	public ExasolTableKeyColumn(AbstractTableConstraint<ExasolTable> constraint, ExasolTableColumn tableColumn, Integer ordinalPosition) {
		
		this.constraint = constraint;
		this.tableColumn = tableColumn;
		this.ordinalPosition = ordinalPosition;
	}
	
	@Override
	public AbstractTableConstraint<ExasolTable> getParentObject() {
		return constraint;
	}

	@Override
	@NotNull
	public DBPDataSource getDataSource() {
		return constraint.getTable().getDataSource();
	}
	
    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @NotNull
    @Override
    @Property(id = "name", viewable = true, order = 1)
    public ExasolTableColumn getAttribute()
    {
        return tableColumn;
    }

    @Override
    @Property(viewable = true, editable = false, order = 3)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }


}
