package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Recycled object
 */
public class OracleRecycledObject extends OracleSchemaObject implements DBSObjectLazy<OracleDataSource> {

    public enum Operation {
        DROP,
        TRUNCATE
    }

    private String recycledName;
    private Operation operation;
    private String objectType;
    private Object tablespace;
    private String createTime;
    private String dropTime;
    private String partitionName;
    private boolean canUndrop;
    private boolean canPurge;

    protected OracleRecycledObject(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "ORIGINAL_NAME"), true);
        this.recycledName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.operation = CommonUtils.valueOf(Operation.class, JDBCUtils.safeGetString(dbResult, "OPERATION"));
        this.objectType = JDBCUtils.safeGetString(dbResult, "TYPE");
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TS_NAME");
        this.createTime = JDBCUtils.safeGetString(dbResult, "CREATETIME");
        this.dropTime = JDBCUtils.safeGetString(dbResult, "DROPTIME");
        this.partitionName = JDBCUtils.safeGetString(dbResult, "PARTITION_NAME");
        this.canUndrop = JDBCUtils.safeGetBoolean(dbResult, "CAN_UNDROP", "Y");
        this.canPurge = JDBCUtils.safeGetBoolean(dbResult, "CAN_PURGE", "Y");
    }

    @Property(name = "Object name", viewable = true, order = 2, description = "New object name")
    public String getRecycledName()
    {
        return recycledName;
    }

    @Property(name = "Operation", viewable = true, order = 3, description = "Operation carried out on the object")
    public Operation getOperation()
    {
        return operation;
    }

    @Property(name = "Object type", viewable = true, order = 4, description = "Type of the object")
    public String getObjectType()
    {
        return objectType;
    }

    @Property(name = "Tablespace", viewable = true, order = 5, description = "Tablespace to which the object belongs")
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Property(name = "Create time", order = 6)
    public String getCreateTime()
    {
        return createTime;
    }

    @Property(name = "Drop time", order = 7)
    public String getDropTime()
    {
        return dropTime;
    }

    @Property(name = "Partition", order = 8)
    public String getPartitionName()
    {
        return partitionName;
    }

    @Property(name = "Can Undrop", viewable = true, order = 9, description = "Indicates whether the object can be undropped")
    public boolean isCanUndrop()
    {
        return canUndrop;
    }

    @Property(name = "Can Purge", viewable = true, order = 10, description = "Indicates whether the object can be purged")
    public boolean isCanPurge()
    {
        return canPurge;
    }

    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

}
