/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Oracle tablespace
 */
public class OracleTablespace extends OracleGlobalObject implements DBSEntity {

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

    final FileCache fileCache = new FileCache();
    final SegmentCache segmentCache = new SegmentCache();

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

    @Association
    public Collection<OracleDataFile> getFiles(DBRProgressMonitor monitor) throws DBException
    {
        return fileCache.getObjects(monitor, this);
    }

    public OracleDataFile getFile(DBRProgressMonitor monitor, long relativeFileNo) throws DBException
    {
        for (OracleDataFile file : fileCache.getObjects(monitor, this)) {
            if (file.getRelativeNo() == relativeFileNo) {
                return file;
            }
        }
        return null;
    }

    @Association
    public Collection<OracleSegment<OracleTablespace>> getSegments(DBRProgressMonitor monitor) throws DBException
    {
        return segmentCache.getObjects(monitor, this);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        fileCache.clearCache();
        segmentCache.clearCache();
        return true;
    }

    static class FileCache extends JDBCObjectCache<OracleTablespace, OracleDataFile> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTablespace owner) throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.DBA_" +
                    (owner.getContents() == Contents.TEMPORARY ? "TEMP" : "DATA") +
                    "_FILES WHERE TABLESPACE_NAME=? ORDER BY FILE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleDataFile fetchObject(JDBCExecutionContext context, OracleTablespace owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataFile(owner, resultSet, owner.getContents() == Contents.TEMPORARY);
        }
    }

    static class SegmentCache extends JDBCObjectCache<OracleTablespace, OracleSegment<OracleTablespace>> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTablespace owner) throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminViewPrefix(owner.getDataSource()) +
                "SEGMENTS WHERE TABLESPACE_NAME=? ORDER BY SEGMENT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSegment<OracleTablespace> fetchObject(JDBCExecutionContext context, OracleTablespace owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSegment<OracleTablespace>(context.getProgressMonitor(), owner, resultSet);
        }
    }

    interface TablespaceReferrer {
        OracleDataSource getDataSource();
        Object getTablespaceReference();
    }

    static Object resolveTablespaceReference(DBRProgressMonitor monitor, TablespaceReferrer referrer) throws DBException
    {
        final OracleDataSource dataSource = referrer.getDataSource();
        if (!dataSource.isAdmin()) {
            return referrer;
        } else {
            final Object reference = referrer.getTablespaceReference();
            if (reference instanceof String) {
                OracleTablespace tablespace = dataSource.tablespaceCache.getObject(monitor, dataSource, (String) reference);
                if (tablespace != null) {
                    return tablespace;
                } else {
                    log.warn("Tablespace '" + reference + "' not found");
                    return reference;
                }
            } else {
                return reference;
            }
        }
    }

    public static class TablespaceReferenceValidator implements IPropertyCacheValidator<TablespaceReferrer> {
        public boolean isPropertyCached(TablespaceReferrer object)
        {
            return
                object.getTablespaceReference() instanceof OracleTablespace ||
                object.getTablespaceReference() == null ||
                object.getDataSource().tablespaceCache.isCached() ||
                !object.getDataSource().isAdmin();
        }
    }

}
