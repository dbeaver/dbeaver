package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

public class ExasolFunction
        extends AbstractProcedure<ExasolDataSource, ExasolSchema> implements DBSProcedure, DBPRefreshableObject, ExasolSourceObject {

    
    private String remarks;
    private String owner;
    private String sql;
    private ExasolSchema exasolSchema;
    private Timestamp createTime;
    
    public ExasolFunction(ExasolSchema schema, ResultSet dbResult)
    {
        super(schema,true);
        this.owner = JDBCUtils.safeGetString(dbResult, "FUNCTION_OWNER");
        this.remarks = JDBCUtils.safeGetString(dbResult, "FUNCTION_COMMENT");
        this.name = JDBCUtils.safeGetString(dbResult, "FUNCTION_NAME");
        this.sql = JDBCUtils.safeGetString(dbResult, "FUNCTION_TEXT");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.exasolSchema = schema;
    }
    public ExasolFunction(ExasolSchema schema)
    {
        super(schema, false);
        exasolSchema = schema;
        sql = "";
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor)
            throws DBException
    {
        getContainer().functionCache.clearCache();
        getContainer().functionCache.getAllObjects(monitor, exasolSchema);
        return getContainer().functionCache.getObject(monitor, exasolSchema, getName());
    }

    
    @Override
    public DBSProcedureType getProcedureType()
    {
        return DBSProcedureType.FUNCTION;
    }

    @Override
    public Collection<? extends DBSProcedureParameter> getParameters(
            DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getContainer(),this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
            throws DBException
    {
        return sql;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        sql = sourceText;
        
    }
    
    // -----------------------
    // Properties
    // -----------------------


    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return this.name;
    }

    @Property(viewable = true, order = 2)
    public ExasolSchema getSchema() {
        return exasolSchema;
    }


    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 11)
    public String getDescription() {
        return this.remarks;
    }
    
    @Override
    public void setDescription(String description)
    {
        this.remarks = description;
    }



    @NotNull
    @Property(hidden = true, editable = true, updatable = true)
    public String getSql() {
        return this.sql;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public Timestamp getCreationTime() {
        return this.createTime;
    }

    @Property(viewable = false, category = ExasolConstants.CAT_OWNER)
    public String getOwner() {
        return owner;
    }



}
