/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

public class YashanDBTablespace extends YashanDBGlobalObject implements DBPRefreshableObject, DBPObjectStatistics {

	private String name;
	private long blockSize;
	private long maxSize;
	private long totalBytes;
	private long userBytes;
	private long userBlocks;
	private Status status;
	private Contents contents;
	private Logging logging;
	private AllocationType allocationType;
	private SegmentSpaceManagement segmentSpaceManagement;
	private String encrypted;
	private volatile Long availableSize;
	private volatile Long usedSize;
	final FileCache fileCache = new FileCache();
	final SegmentCache segmentCache = new SegmentCache();

	protected YashanDBTablespace(YashanDBDataSource dataSource, ResultSet dbResult) {
		super(dataSource, true);
		this.name = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
		this.blockSize = JDBCUtils.safeGetLong(dbResult, "BLOCK_SIZE");
		this.maxSize = JDBCUtils.safeGetLong(dbResult, "MAX_SIZE");
		this.totalBytes = JDBCUtils.safeGetLong(dbResult, "TOTAL_BYTES");
		this.userBytes = JDBCUtils.safeGetLong(dbResult, "USER_BYTES");
		this.userBlocks = JDBCUtils.safeGetLong(dbResult, "USER_BLOCKS");
		this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetString(dbResult, "STATUS"), Status.OFFLINE,
				true);
		this.contents = CommonUtils.valueOf(Contents.class, JDBCUtils.safeGetString(dbResult, "CONTENTS"), null, true);
		this.logging = CommonUtils.valueOf(Logging.class, JDBCUtils.safeGetString(dbResult, "LOGGING"), null, true);
		this.allocationType = CommonUtils.valueOf(AllocationType.class,
				JDBCUtils.safeGetString(dbResult, "ALLOCATION_TYPE"), null, true);
		this.encrypted = JDBCUtils.safeGetString(dbResult, "ENCRYPTED");
		this.segmentSpaceManagement = CommonUtils.valueOf(SegmentSpaceManagement.class,
				JDBCUtils.safeGetString(dbResult, "SEGMENT_SPACE_MANAGEMENT"), null, true);
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 4, formatter = ByteNumberFormat.class)
	public Long getAvailableSize(DBRProgressMonitor monitor) throws DBException {
		if (availableSize == null) {
			loadSizes(monitor);
		}
		return availableSize;
	}

	@Property(viewable = true, order = 5, formatter = ByteNumberFormat.class)
	public Long getUsedSize(DBRProgressMonitor monitor) throws DBException {
		if (usedSize == null) {
			loadSizes(monitor);
		}
		return usedSize;
	}

	@Property(viewable = true, editable = true, order = 22, formatter = ByteNumberFormat.class)
	public long getBlockSize() {
		return blockSize;
	}

	@Property(editable = true, order = 23)
	public long getMaxSize() {
		return maxSize;
	}

	@Property(editable = true, order = 24)
	public long getTotalBytes() {
		return totalBytes;
	}

	@Property(editable = true, order = 25)
	public long getUserBytes() {
		return userBytes;
	}

	@Property(editable = true, order = 26)
	public long getUserBlocks() {
		return userBlocks;
	}

	@Property(viewable = true, editable = true, order = 27)
	public Status getStatus() {
		return status;
	}

	@Property(editable = true, order = 28)
	public Contents getContents() {
		return contents;
	}

	@Property(editable = true, order = 29)
	public Logging isLogging() {
		return logging;
	}

	@Property(editable = true, order = 30)
	public AllocationType getAllocationType() {
		return allocationType;
	}

	@Property(editable = true, order = 31)
	public SegmentSpaceManagement getSegmentSpaceManagement() {
		return segmentSpaceManagement;
	}

	@Property(editable = true, order = 32)
	public String getEncrypted() {
		return encrypted;
	}

	@Association
	public Collection<YashanDBDataFile> getFiles(DBRProgressMonitor monitor) throws DBException {
		return fileCache.getAllObjects(monitor, this);
	}

	public YashanDBDataFile getFile(DBRProgressMonitor monitor, long relativeFileNo) throws DBException {
		if (fileCache.getAllObjects(monitor, this) != null) {
			return fileCache.getAllObjects(monitor, this).get((int) relativeFileNo);
		}
		return null;
	}

	@Association
	public Collection<YashanDBSegment<YashanDBTablespace>> getSegments(DBRProgressMonitor monitor) throws DBException {
		return segmentCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		availableSize = null;
		usedSize = null;
		fileCache.clearCache();
		segmentCache.clearCache();
		return this;
	}

	@Override
	public boolean hasStatistics() {
		return usedSize != null;
	}

	@Override
	public long getStatObjectSize() {
		return usedSize == null ? 0 : usedSize;
	}

	private void loadSizes(DBRProgressMonitor monitor) throws DBException {
		try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
				"Load tablespace '" + getName() + "' statistics")) {
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM\n"
					+ "(SELECT SUM(F.BYTES) AVAILABLE_SPACE FROM "
					+ YashanDBUtils.isAdminPriv(getDataSource(), "DATA_FILES") + " F WHERE F.TABLESPACE_NAME=?) XDF,\n"
					+ "(SELECT SUM(S.BYTES) USED_SPACE FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "SEGMENTS")
					+ " S WHERE S.TABLESPACE_NAME=?) XS")) {
				dbStat.setString(1, getName());
				dbStat.setString(2, getName());
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						fetchSizes(dbResult);
					}
				}
			}
		} catch (SQLException e) {
			throw new DBException("Can't read tablespace statistics", e);
		}
	}

	void fetchSizes(JDBCResultSet dbResult) throws SQLException {
		availableSize = dbResult.getLong("AVAILABLE_SPACE");
		usedSize = dbResult.getLong("USED_SPACE");
	}

	static class FileCache extends JDBCObjectCache<YashanDBTablespace, YashanDBDataFile> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBTablespace owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ YashanDBUtils.isAdminPriv(owner.getDataSource(),
							(owner.getContents() == Contents.TEMPORARY ? "TEMP" : "DATA"))
					+ "_FILES WHERE TABLESPACE_NAME=? ORDER BY FILE_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBDataFile fetchObject(@NotNull JDBCSession session, @NotNull YashanDBTablespace owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBDataFile(owner, resultSet, owner.getContents() == Contents.TEMPORARY);
		}
	}

	static class SegmentCache extends JDBCObjectCache<YashanDBTablespace, YashanDBSegment<YashanDBTablespace>> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBTablespace owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "SEGMENTS")
							+ " WHERE TABLESPACE_NAME=? ORDER BY SEGMENT_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBSegment<YashanDBTablespace> fetchObject(@NotNull JDBCSession session,
				@NotNull YashanDBTablespace owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBSegment<>(session.getProgressMonitor(), owner, resultSet);
		}
	}

	static Object resolveTablespaceReference(DBRProgressMonitor monitor, DBSObjectLazy<YashanDBDataSource> referrer,
			@Nullable Object propertyId) throws DBException {
		final YashanDBDataSource dataSource = referrer.getDataSource();
		if (!dataSource.isAdmin()) {
			return referrer.getLazyReference(propertyId);
		} else {
			return YashanDBUtils.resolveLazyReference(monitor, dataSource, dataSource.tablespaceCache, referrer,
					propertyId);
		}
	}

	public static class TablespaceReferenceValidator
			implements IPropertyCacheValidator<DBSObjectLazy<YashanDBDataSource>> {
		@Override
		public boolean isPropertyCached(DBSObjectLazy<YashanDBDataSource> object, Object propertyId) {
			return object.getLazyReference(propertyId) instanceof YashanDBTablespace
					|| object.getLazyReference(propertyId) == null
					|| object.getDataSource().tablespaceCache.isFullyCached() || !object.getDataSource().isAdmin();
		}
	}

	public enum Status {
		ONLINE, OFFLINE,
	}

	public enum Contents {
		PERMANENT, TEMPORARY, UNDO, SWAP
	}

	public enum Logging {
		LOGGING, NOLOGGING,
	}

	public enum AllocationType {
		UNIFORM, AUTO
	}

	public enum SegmentSpaceManagement {
		BITMAP
	}
}
