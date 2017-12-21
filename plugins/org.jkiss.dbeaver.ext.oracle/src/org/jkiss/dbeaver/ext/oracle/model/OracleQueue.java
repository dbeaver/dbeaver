package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 * Oracle sequence
 */
public class OracleQueue extends OracleSchemaObject {

    private static final Log log = Log.getLog(OracleQueue.class);

    public static enum QueueType {
        NORMAL_QUEUE,
        EXCEPTION_QUEUE
    };

    private String queueTable;
    private int qId;
    private QueueType queueType;
    private Integer maxRetries;
    private Integer retryDelay;
    private String enqueueEnabled;
    private String dequeueEnabled;
    private String retention;
    private String userComment;
    private String networkName;

    public OracleQueue(OracleSchema schema, String name) {
        super(schema, name, false);
    }

    public OracleQueue(OracleSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "NAME"), true);
        this.queueTable = JDBCUtils.safeGetString(dbResult, "QUEUE_TABLE");
        this.queueType = QueueType.valueOf(JDBCUtils.safeGetString(dbResult, "QUEUE_TYPE"));
        this.maxRetries = JDBCUtils.safeGetInteger(dbResult, "MAX_RETRIES");
        this.retryDelay = JDBCUtils.safeGetInteger(dbResult, "RETRY_DELAY");
        this.qId = JDBCUtils.safeGetInt(dbResult, "QID");
        this.enqueueEnabled = JDBCUtils.safeGetString(dbResult, "ENQUEUE_ENABLED");
        this.dequeueEnabled = JDBCUtils.safeGetString(dbResult, "DEQUEUE_ENABLED");
        this.retention = JDBCUtils.safeGetString(dbResult, "RETENTION");
        this.userComment = JDBCUtils.safeGetString(dbResult, "USER_COMMENT");
        this.networkName = JDBCUtils.safeGetString(dbResult, "NETWORK_NAME");
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public OracleTableBase getQueueTable(DBRProgressMonitor monitor) throws DBException {
        return this.parent.tableCache.getObject(monitor, parent, queueTable);
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public int getQId() {
        return qId;
    }

    @Property(viewable = true, order = 4)
    public QueueType getQueueType() {
        return queueType;
    }

    @Property(viewable = true, order = 5)
    public Integer getMaxRetries() {
        return maxRetries;
    }

    @Property(viewable = true, order = 6)
    public Integer getRetryDelay() {
        return retryDelay;
    }

    @Property(viewable = true, order = 7)
    public String getEnqueueEnabled() {
        return enqueueEnabled;
    }

    @Property(viewable = true, order = 8)
    public String getDequeueEnabled() {
        return dequeueEnabled;
    }

    @Property(viewable = true, order = 9)
    public String getRetention() {
        return retention;
    }

    @Property(viewable = true, order = 10)
    public String getUserComment() {
        return userComment;
    }

    @Property(viewable = true, order = 11)
    public String getNetworkName() {
        return networkName;
    }
}
