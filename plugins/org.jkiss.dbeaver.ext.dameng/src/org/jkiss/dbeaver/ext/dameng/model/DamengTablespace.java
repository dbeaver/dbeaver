/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengTablespace implements DBPRefreshableObject, DBPObjectStatistics, DBPScriptObject, DBPObjectWithLongId {

    private final DamengDataSource dataSource;

    private long id;

    private final String name;

    private Cache cache;

    private Type type;

    private Status status;

    private Long maxSize;

    private Long totalSize;

    private Integer fileNum;

    private String encryptName;
    private String encryptedKey;

    private Integer copyNum;

    private String sizeMode;

    private Integer optNode;

    private final Long usedSize;

    final FileCache fileCache = new FileCache();

    public DamengTablespace(DamengDataSource dataSource, JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.id = JDBCUtils.safeGetInt(dbResult, DamengConstants.ID);
        this.name = JDBCUtils.safeGetString(dbResult, DamengConstants.NAME);
        this.cache = CommonUtils.valueOf(
                Cache.class,
                JDBCUtils.safeGetString(dbResult, "CACHE"),
                Cache.NORMAL);
        int typeValue = JDBCUtils.safeGetInt(dbResult, DamengConstants.TYPE$);
        this.type = Type.values()[typeValue - 1];
        int statusValue = JDBCUtils.safeGetInt(dbResult, DamengConstants.STATUS$);
        this.status = Status.values()[statusValue];
        this.maxSize = JDBCUtils.safeGetLong(dbResult, "MAX_SIZE");
        this.totalSize = JDBCUtils.safeGetLong(dbResult, "TOTAL_SIZE");
        this.fileNum = JDBCUtils.safeGetInt(dbResult, "FILE_NUM");
        this.encryptName = JDBCUtils.safeGetString(dbResult, "ENCRYPT_NAME");
        this.encryptedKey = JDBCUtils.safeGetString(dbResult, "ENCRYPTED_KEY");
        this.copyNum = JDBCUtils.safeGetInteger(dbResult, "COPY_NUM");
        this.sizeMode = JDBCUtils.safeGetString(dbResult, "SIZE_MODE");
        this.optNode = JDBCUtils.safeGetInteger(dbResult, "OPT_NODE");
        this.usedSize = JDBCUtils.safeGetLong(dbResult, "USED_SIZE");
    }

    @Association
    public Collection<DamengDataFile> getFiles(DBRProgressMonitor monitor) throws DBException {
        return fileCache.getAllObjects(monitor, this);
    }

    @Override
    @Property(viewable = true)
    public long getObjectId() {
        return id;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean hasStatistics() {
        return usedSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return usedSize;
    }

    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        fileCache.clearCache();
        return this;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @Override
    public DamengDataSource getDataSource() {
        return dataSource;
    }


    @Property(viewable = true)
    public Status getStatus() {
        return status;
    }

    @Property(viewable = true)
    public Cache getCache() {
        return cache;
    }

    @Property(viewable = true)
    public Type getType() {
        return type;
    }

    @Property(viewable = true)
    public Long getMaxSize() {
        return maxSize;
    }

    @Property(viewable = true)
    public Long getTotalSize() {
        return totalSize;
    }

    @Property(viewable = true)
    public Integer getFileNum() {
        return fileNum;
    }

    @Property(viewable = true)
    public String getEncryptName() {
        return encryptName;
    }

    @Property(viewable = true)
    public String getEncryptedKey() {
        return encryptedKey;
    }

    @Property(viewable = true)
    public Integer getCopyNum() {
        return copyNum;
    }

    @Property(viewable = true)
    public String getSizeMode() {
        return sizeMode;
    }

    @Property(viewable = true)
    public Integer getOptNode() {
        return optNode;
    }

    @Property(viewable = true)
    public Long getUsedSize() {
        return usedSize;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DamengUtils.getDDL(monitor, this, DamengConstants.ObjectType.TABLESPACE, null);
    }

    static class FileCache extends JDBCObjectCache<DamengTablespace, DamengDataFile> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengTablespace damengTablespace) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT df.* " +
                    "FROM V$TABLESPACE AS ts, V$DATAFILE AS df " +
                    "WHERE ts.ID = df.GROUP_ID AND ts.NAME = ?");
            dbStat.setString(1, damengTablespace.getName());
            return dbStat;
        }

        @Override
        protected DamengDataFile fetchObject(@NotNull JDBCSession session, @NotNull DamengTablespace damengTablespace, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengDataFile(damengTablespace, resultSet);
        }
    }

    public enum Type {
        PERMANENT,
        TEMPORARY,
    }

    public enum Status {
        ONLINE,
        OFFLINE,
        RES_OFFLINE,
        CORRUPT,
    }

    public enum Cache {
        NORMAL,
        KEEP,
    }
}
