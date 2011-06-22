package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Recycled object
 */
public class OracleRecycledObject extends OracleSchemaObject {

    public enum Operation {
        DROP,
        TRUNCATE
    }

    private String originalName;
    private Operation operation;
    private String objectType;
    private String tablespaceName;
    private String createTime;
    private String dropTime;
    private String partitionName;
    private boolean canUndrop;
    private boolean canPurge;

    protected OracleRecycledObject(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
        this.originalName = JDBCUtils.safeGetString(dbResult, "ORIGINAL_NAME");
        this.operation = CommonUtils.valueOf(Operation.class, JDBCUtils.safeGetString(dbResult, "OPERATION"));
        this.objectType = JDBCUtils.safeGetString(dbResult, "TYPE");
        this.tablespaceName = JDBCUtils.safeGetString(dbResult, "TS_NAME");
        this.createTime = JDBCUtils.safeGetString(dbResult, "CREATETIME");
        this.dropTime = JDBCUtils.safeGetString(dbResult, "DROPTIME");
        this.partitionName = JDBCUtils.safeGetString(dbResult, "PARTITION_NAME");
        this.canUndrop = JDBCUtils.safeGetBoolean(dbResult, "CAN_UNDROP", "Y");
        this.canPurge = JDBCUtils.safeGetBoolean(dbResult, "CAN_PURGE", "Y");
    }

    @Property(name = "Original name", viewable = true, order = 2)
    public String getOriginalName()
    {
        return originalName;
    }

    @Property(name = "Operation", viewable = true, order = 3)
    public Operation getOperation()
    {
        return operation;
    }

    @Property(name = "Object name", viewable = true, order = 4)
    public String getObjectType()
    {
        return objectType;
    }

    @Property(name = "Tablespace", viewable = true, order = 5)
    public String getTablespaceName()
    {
        return tablespaceName;
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

    @Property(name = "Can Undrop", order = 9)
    public boolean isCanUndrop()
    {
        return canUndrop;
    }

    @Property(name = "Can Purge", order = 10)
    public boolean isCanPurge()
    {
        return canPurge;
    }
}
