/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * @author Karl Griesser
 *
 */
public class ExasolTableUniqueKey extends JDBCTableConstraint<ExasolTable> {
	
	private String owner;
	private Boolean enabled;
	
	private List<ExasolTableKeyColumn> columns;
	
	
	// CONSTRUCTOR
	
	public ExasolTableUniqueKey(DBRProgressMonitor monitor, ExasolTable table, ResultSet dbResult, DBSEntityConstraintType type)
	throws DBException
	{
		super(table,JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),null,type,true);
		this.owner = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_OWNER");
		this.enabled = JDBCUtils.safeGetBoolean(dbResult, "CONSTRAINT_ENABLED");
		
	}
	
	public ExasolTableUniqueKey(ExasolTable exasolTable, DBSEntityConstraintType constraintType)
	{
		super(exasolTable,null,null,constraintType,false);
	}
	
    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return columns;
    }

    public void setColumns(List<ExasolTableKeyColumn> columns)
    {
        this.columns = columns;
    }

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public ExasolTable getTable()
    {
        return super.getTable();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 3)
    public DBSEntityConstraintType getConstraintType()
    {
        return super.getConstraintType();
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false, order = 4)
    public String getDescription()
    {
        return null;
    }

    @Property(viewable = false, editable = false, category = ExasolConstants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false)
    public Boolean getEnabled()
    {
        return enabled;
    }



    
    
}
