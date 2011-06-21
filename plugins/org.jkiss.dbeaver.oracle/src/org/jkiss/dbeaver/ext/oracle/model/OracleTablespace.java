/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Oracle tablespace
 */
public class OracleTablespace extends OracleGlobalObject {

    public enum Status {
        ONLINE,
        OFFLINE,
        READ_ONLY
    }

    public enum Contents {
        PERMANENT,
        TEMPORARY,
        UNDO
    }

    public enum Logging {
        LOGGING,
        NOLOGGING,
    }

    public enum ExtentManagement {
        DICTIONARY,
        LOCAL
    }

    public enum AllocationType {
        SYSTEM,
        UNIFORM,
        USER,
    }

    public enum SegmentSpaceManagement {
        MANUAL,
        AUTO
    }

    public enum Retention {
        GUARANTEE,
        NOGUARANTEE,
        NOT_APPLY
    }

    private String name;
    private long blockSize;
    private long initialExtent;
    private long nextExtent;
    private long minExtents;
    private long maxExtents;
    private long pctIncrease;
    private long minExtLen;
    private Status status;
    private Contents contents;
    private Logging logging;
    private boolean forceLogging;
    private ExtentManagement extentManagement;
    private AllocationType allocationType;
    private boolean pluggedIn;
    private SegmentSpaceManagement segmentSpaceManagement;
    private boolean defTableCompression;
    private Retention retention;
    private boolean bigFile;

    protected OracleTablespace(OracleDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, dbResult != null);
        this.name = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
        this.blockSize = JDBCUtils.safeGetLong(dbResult, "BLOCK_SIZE");
        this.initialExtent = JDBCUtils.safeGetLong(dbResult, "INITIAL_EXTENT");
        this.nextExtent = JDBCUtils.safeGetLong(dbResult, "NEXT_EXTENT");
        this.minExtents = JDBCUtils.safeGetLong(dbResult, "MIN_EXTENTS");
        this.maxExtents = JDBCUtils.safeGetLong(dbResult, "MAX_EXTENTS");
        this.pctIncrease = JDBCUtils.safeGetLong(dbResult, "PCT_INCREASE");
        this.minExtLen = JDBCUtils.safeGetLong(dbResult, "MIN_EXTLEN");
        this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetString(dbResult, "STATUS"), true);
        this.contents = CommonUtils.valueOf(Contents.class, JDBCUtils.safeGetString(dbResult, "CONTENTS"), true);
        this.logging = CommonUtils.valueOf(Logging.class, JDBCUtils.safeGetString(dbResult, "LOGGING"), true);
        this.forceLogging = JDBCUtils.safeGetBoolean(dbResult, "FORCE_LOGGING", "Y");
        this.extentManagement = CommonUtils.valueOf(ExtentManagement.class, JDBCUtils.safeGetString(dbResult, "EXTENT_MANAGEMENT"), true);
        this.allocationType = CommonUtils.valueOf(AllocationType.class, JDBCUtils.safeGetString(dbResult, "ALLOCATION_TYPE"), true);
        this.pluggedIn = JDBCUtils.safeGetBoolean(dbResult, "PLUGGED_IN", "Y");
        this.segmentSpaceManagement = CommonUtils.valueOf(SegmentSpaceManagement.class, JDBCUtils.safeGetString(dbResult, "SEGMENT_SPACE_MANAGEMENT"), true);
        this.defTableCompression = "ENABLED".equals(JDBCUtils.safeGetString(dbResult, "DEF_TAB_COMPRESSION"));
        this.retention = CommonUtils.valueOf(Retention.class, JDBCUtils.safeGetString(dbResult, "RETENTION"), true);
        this.bigFile = JDBCUtils.safeGetBoolean(dbResult, "BIGFILE", "Y");
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Block Size", viewable = true, editable = true, order = 2)
    public long getBlockSize()
    {
        return blockSize;
    }

    @Property(name = "Initial Extent", editable = true, order = 3)
    public long getInitialExtent()
    {
        return initialExtent;
    }

    @Property(name = "Next Extent", editable = true, order = 4)
    public long getNextExtent()
    {
        return nextExtent;
    }

    @Property(name = "Min Extents", editable = true, order = 5)
    public long getMinExtents()
    {
        return minExtents;
    }

    @Property(name = "Max Extents", editable = true, order = 6)
    public long getMaxExtents()
    {
        return maxExtents;
    }

    @Property(name = "PCT Increase", editable = true, order = 7)
    public long getPctIncrease()
    {
        return pctIncrease;
    }

    @Property(name = "Min Ext Len", editable = true, order = 8)
    public long getMinExtLen()
    {
        return minExtLen;
    }

    @Property(name = "Status", viewable = true, editable = true, order = 9)
    public Status getStatus()
    {
        return status;
    }

    @Property(name = "Contents", editable = true, order = 10)
    public Contents getContents()
    {
        return contents;
    }

    @Property(name = "Logging", editable = true, order = 11)
    public Logging isLogging()
    {
        return logging;
    }

    @Property(name = "Force Logging", editable = true, order = 12)
    public boolean isForceLogging()
    {
        return forceLogging;
    }

    @Property(name = "Extent Management", editable = true, order = 13)
    public ExtentManagement getExtentManagement()
    {
        return extentManagement;
    }

    @Property(name = "Allocation Type", editable = true, order = 14)
    public AllocationType getAllocationType()
    {
        return allocationType;
    }

    @Property(name = "Plugged In", editable = true, order = 15)
    public boolean isPluggedIn()
    {
        return pluggedIn;
    }

    @Property(name = "Segment Space Management", editable = true, order = 16)
    public SegmentSpaceManagement getSegmentSpaceManagement()
    {
        return segmentSpaceManagement;
    }

    @Property(name = "Default Table Compression", editable = true, order = 17)
    public boolean isDefTableCompression()
    {
        return defTableCompression;
    }

    @Property(name = "Retention", editable = true, order = 18)
    public Retention getRetention()
    {
        return retention;
    }

    @Property(name = "Big File", editable = true, order = 19)
    public boolean isBigFile()
    {
        return bigFile;
    }
}
