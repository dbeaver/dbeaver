package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

public class AltibaseReplicationItem extends AltibaseObject<AltibaseReplication> {

    private String tableOid;
    
    private String replObjFrom;
    private String replObjTo;

    //private boolean isPartitionedTable;
    private boolean isPartitionedRepl;
    private long invalidMaxSn;

    protected AltibaseReplicationItem(AltibaseReplication parent, JDBCResultSet resultSet) {
        super(parent, String.valueOf(JDBCUtils.safeGetLong(resultSet, "TABLE_OID")), true);

        tableOid = JDBCUtils.safeGetString(resultSet, "TABLE_OID");
        
        //isPartitionedTable = JDBCUtils.safeGetBoolean(resultSet, "IS_PARTITION", "Y");
        isPartitionedRepl = JDBCUtils.safeGetBoolean(resultSet, "REPLICATION_UNIT", "P");
        
        invalidMaxSn = JDBCUtils.safeGetLong(resultSet, "INVALID_MAX_SN");
        
        replObjFrom = getDottedName(JDBCUtils.safeGetString(resultSet, "LOCAL_USER_NAME"),
                JDBCUtils.safeGetString(resultSet, "LOCAL_TABLE_NAME"),
                JDBCUtils.safeGetString(resultSet, "LOCAL_PARTITION_NAME"));
        
        replObjTo = getDottedName(JDBCUtils.safeGetString(resultSet, "REMOTE_USER_NAME"),
                JDBCUtils.safeGetString(resultSet, "REMOTE_TABLE_NAME"),
                JDBCUtils.safeGetString(resultSet, "REMOTE_PARTITION_NAME"));
    }
    
    private String getDottedName(String schema, String table, String partition) {
        return new StringBuilder().append(schema).append(".").append(table)
                .append(CommonUtils.isEmpty(partition) ? "" : "." + partition).toString();
    }

    @NotNull
    @Override
    @Property(viewable = false, order = 1, hidden = true)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 2)
    public String getTableOid() {
        return tableOid;
    }
    
    @NotNull
    @Property(viewable = true, order = 5)
    public String getReplObjFrom() {
        return replObjFrom;
    }
    
    @NotNull
    @Property(viewable = true, order = 6)
    public String getReplObjTo() {
        return replObjTo;
    }
    
    @Property(viewable = true, order = 10)
    public boolean getIsPartitionedRepl() {
        return isPartitionedRepl;
    }
    
    @Property(viewable = true, order = 11)
    public long getInvalidMaxSn() {
        return invalidMaxSn;
    }
}
