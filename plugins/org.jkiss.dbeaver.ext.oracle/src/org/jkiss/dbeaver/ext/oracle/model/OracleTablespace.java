/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Oracle tablespace
 */
public class OracleTablespace extends OracleGlobalObject implements DBPRefreshableObject
{

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
        super(dataSource, true);
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

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = true, order = 2)
    public long getBlockSize()
    {
        return blockSize;
    }

    @Property(editable = true, order = 3)
    public long getInitialExtent()
    {
        return initialExtent;
    }

    @Property(editable = true, order = 4)
    public long getNextExtent()
    {
        return nextExtent;
    }

    @Property(editable = true, order = 5)
    public long getMinExtents()
    {
        return minExtents;
    }

    @Property(editable = true, order = 6)
    public long getMaxExtents()
    {
        return maxExtents;
    }

    @Property(editable = true, order = 7)
    public long getPctIncrease()
    {
        return pctIncrease;
    }

    @Property(editable = true, order = 8)
    public long getMinExtLen()
    {
        return minExtLen;
    }

    @Property(viewable = true, editable = true, order = 9)
    public Status getStatus()
    {
        return status;
    }

    @Property(editable = true, order = 10)
    public Contents getContents()
    {
        return contents;
    }

    @Property(editable = true, order = 11)
    public Logging isLogging()
    {
        return logging;
    }

    @Property(editable = true, order = 12)
    public boolean isForceLogging()
    {
        return forceLogging;
    }

    @Property(editable = true, order = 13)
    public ExtentManagement getExtentManagement()
    {
        return extentManagement;
    }

    @Property(editable = true, order = 14)
    public AllocationType getAllocationType()
    {
        return allocationType;
    }

    @Property(editable = true, order = 15)
    public boolean isPluggedIn()
    {
        return pluggedIn;
    }

    @Property(editable = true, order = 16)
    public SegmentSpaceManagement getSegmentSpaceManagement()
    {
        return segmentSpaceManagement;
    }

    @Property(editable = true, order = 17)
    public boolean isDefTableCompression()
    {
        return defTableCompression;
    }

    @Property(editable = true, order = 18)
    public Retention getRetention()
    {
        return retention;
    }

    @Property(editable = true, order = 19)
    public boolean isBigFile()
    {
        return bigFile;
    }

    @Association
    public Collection<OracleDataFile> getFiles(DBRProgressMonitor monitor) throws DBException
    {
        return fileCache.getAllObjects(monitor, this);
    }

    public OracleDataFile getFile(DBRProgressMonitor monitor, long relativeFileNo) throws DBException
    {
        for (OracleDataFile file : fileCache.getAllObjects(monitor, this)) {
            if (file.getRelativeNo() == relativeFileNo) {
                return file;
            }
        }
        return null;
    }

    @Association
    public Collection<OracleSegment<OracleTablespace>> getSegments(DBRProgressMonitor monitor) throws DBException
    {
        return segmentCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        fileCache.clearCache();
        segmentCache.clearCache();
        return this;
    }

    static class FileCache extends JDBCObjectCache<OracleTablespace, OracleDataFile> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleTablespace owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.DBA_" +
                    (owner.getContents() == Contents.TEMPORARY ? "TEMP" : "DATA") +
                    "_FILES WHERE TABLESPACE_NAME=? ORDER BY FILE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleDataFile fetchObject(@NotNull JDBCSession session, @NotNull OracleTablespace owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataFile(owner, resultSet, owner.getContents() == Contents.TEMPORARY);
        }
    }

    static class SegmentCache extends JDBCObjectCache<OracleTablespace, OracleSegment<OracleTablespace>> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleTablespace owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminViewPrefix(owner.getDataSource()) +
                "SEGMENTS WHERE TABLESPACE_NAME=? ORDER BY SEGMENT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSegment<OracleTablespace> fetchObject(@NotNull JDBCSession session, @NotNull OracleTablespace owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSegment<>(session.getProgressMonitor(), owner, resultSet);
        }
    }

    static Object resolveTablespaceReference(DBRProgressMonitor monitor, DBSObjectLazy<OracleDataSource> referrer, @Nullable Object propertyId) throws DBException
    {
        final OracleDataSource dataSource = referrer.getDataSource();
        if (!dataSource.isAdmin()) {
            return referrer.getLazyReference(propertyId);
        } else {
            return OracleUtils.resolveLazyReference(monitor, dataSource, dataSource.tablespaceCache, referrer, propertyId);
        }
    }

    public static class TablespaceReferenceValidator implements IPropertyCacheValidator<DBSObjectLazy<OracleDataSource>> {
        @Override
        public boolean isPropertyCached(DBSObjectLazy<OracleDataSource> object, Object propertyId)
        {
            return
                object.getLazyReference(propertyId) instanceof OracleTablespace ||
                object.getLazyReference(propertyId) == null ||
                object.getDataSource().tablespaceCache.isFullyCached() ||
                !object.getDataSource().isAdmin();
        }
    }

}
