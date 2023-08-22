package org.jkiss.dbeaver.ext.dm.model;

import java.util.List;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

public class DmTableForeignKeyColumn extends DmTableConstraintColumn implements DBSTableForeignKeyColumn {

	public DmTableForeignKeyColumn(DmTableForeignKey constraint, DmTableColumn tableColumn, int ordinalPosition) {
		super(constraint, tableColumn, ordinalPosition);
	}

	@Override
	@Property(id = "reference", viewable = true, order = 4)
	public DmTableColumn getReferencedColumn() {
		DmTableConstraint referencedConstraint = ((DmTableForeignKey) getParentObject()).getReferencedConstraint();
		if (referencedConstraint != null) {
			List<DmTableConstraintColumn> ar = referencedConstraint.getAttributeReferences(new VoidProgressMonitor());
			if (ar != null) {
				return ar.get(getOrdinalPosition() - 1).getAttribute();
			}
		}
		return null;
	}
}
