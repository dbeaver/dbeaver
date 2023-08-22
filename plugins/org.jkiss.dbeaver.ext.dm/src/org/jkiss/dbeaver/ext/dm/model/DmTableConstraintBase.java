package org.jkiss.dbeaver.ext.dm.model;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * DM Table Constraint Base
 * 
 * @author caosw
 *
 */
public abstract class DmTableConstraintBase extends JDBCTableConstraint<DmTableBase> {

	private static final Log log = Log.getLog(DmTableConstraintBase.class);

	private DmObjectStatus status;
	private List<DmTableConstraintColumn> columns;

	public DmTableConstraintBase(DmTableBase dmTable, String name, DBSEntityConstraintType constraintType,
			DmObjectStatus status, boolean persisted) {
		super(dmTable, name, null, constraintType, persisted);
		this.status = status;
	}

	protected DmTableConstraintBase(DmTableBase dmTable, String name, String description,
			DBSEntityConstraintType constraintType, boolean persisted) {
		super(dmTable, name, description, constraintType, persisted);
	}

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return getTable().getDataSource();
	}

	@NotNull
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	@Override
	public DBSEntityConstraintType getConstraintType() {
		return constraintType;
	}

	@Property(viewable = true, editable = false, order = 9)
	public DmObjectStatus getStatus() {
		return status;
	}

	@Override
	public List<DmTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor) {
		return columns;
	}

	public void addColumn(DmTableConstraintColumn column) {
		if (columns == null) {
			columns = new ArrayList<>();
		}
		this.columns.add(column);
	}

	void setColumns(List<DmTableConstraintColumn> columns) {
		this.columns = columns;
	}
}
