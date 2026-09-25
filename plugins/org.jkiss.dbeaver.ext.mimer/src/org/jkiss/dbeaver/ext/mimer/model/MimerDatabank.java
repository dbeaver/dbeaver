/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Mimer SQL databank - a physical storage unit (Mimer SQL's tablespace analogue). Read from
 * {@code INFORMATION_SCHEMA.EXT_DATABANKS} (file #1's row). Backs {@code CREATE}/
 * {@code ALTER DATABANK} for {@link org.jkiss.dbeaver.ext.mimer.edit.MimerDatabankManager};
 * {@code fileSize} stays create-only since the catalog has no "current size" column to
 * diff a property-grid edit against.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabank implements DBSObject, DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject, DBPImageProvider {

    private final MimerDataSource dataSource;
    private final FileCache fileCache = new FileCache();
    private final ShadowCache shadowCache = new ShadowCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final MimerDatabankPrivilege.PrivilegeCache privilegeCache = new MimerDatabankPrivilege.PrivilegeCache();
    private String name;
    private String creator;
    private String type;
    private boolean online;
    private String onlineTransition = MimerUtils.ONLINE_NOCHANGE;
    private boolean persisted;

    private String file;
    private String minSize;
    private String goalSize;
    private String maxSize;
    private boolean removable;
    private int fileCount = 1;

    // EXT_DATABANKS has no queryable "current file size" column, so this is never re-hydrated on
    // load (always shows blank for an existing databank) - but it IS settable: CREATE DATABANK
    // FILESIZE, and ALTER DATABANK SET/DROP FILESIZE (single-file only, like the other sizes).
    private String fileSize;

    private String comment;

    public MimerDatabank(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "DATABANK_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "DATABANK_CREATOR");
        this.type = JDBCUtils.safeGetStringTrimmed(dbResult, "DATABANK_TYPE");
        this.online = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_ONLINE"));
        this.file = JDBCUtils.safeGetString(dbResult, "FILE_NAME");
        this.minSize = MimerDatabankFile.formatSize(JDBCUtils.safeGetLongNullable(dbResult, "MINSIZE"));
        this.goalSize = MimerDatabankFile.formatSize(JDBCUtils.safeGetLongNullable(dbResult, "GOALSIZE"));
        this.maxSize = MimerDatabankFile.formatSize(JDBCUtils.safeGetLongNullable(dbResult, "MAXSIZE"));
        this.removable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_REMOVABLE"));
        this.fileCount = JDBCUtils.safeGetInt(dbResult, "FILE_COUNT");
        this.persisted = true;
    }

    public MimerDatabank(@NotNull MimerDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
        this.type = "TRANSACTION";
        this.online = true;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2, listProvider = OptionListProvider.class)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Property(viewable = true, order = 3)
    public String getCreator() {
        return creator;
    }

    /**
     * The databank's real, catalog-persisted online/offline state ({@code EXT_DATABANKS.IS_ONLINE})
     * - a read-only checkbox. Change it via the {@link #getOnlineTransition() "Change state"}
     * dropdown next to it, or the "Set Online State" navigator action.
     */
    @Property(viewable = true, order = 4)
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * "Change state" action dropdown - {@link MimerUtils#ONLINE_NOCHANGE} (no-op) by default, its
     * other options gated on the current {@link #isOnline() state} (online → only {@code OFFLINE};
     * offline → {@code ONLINE PRESERVE LOG} / {@code ONLINE RESET LOG}). Picking a non-sentinel
     * value emits the matching {@code SET DATABANK} statement on save; reverts to the sentinel on
     * the next refresh. Same statement family as the "Set Online State" navigator action.
     */
    @Property(viewable = true, editable = true, updatable = true, order = 5, listProvider = OnlineTransitionListProvider.class)
    public String getOnlineTransition() {
        return onlineTransition;
    }

    public void setOnlineTransition(String onlineTransition) {
        this.onlineTransition = onlineTransition;
    }

    /**
     * {@code SET DATABANK "n" <state>} for the {@link #getOnlineTransition() change-state}
     * dropdown's current pick, or {@code null} for the "no change" sentinel.
     */
    @Nullable
    public String buildSetOnlineDDL() {
        return MimerUtils.buildSetOnlineDDL("DATABANK", List.of(name), onlineTransition);
    }

    /**
     * The databank's file - shown/editable here only for a **single-file** databank
     * ({@code ALTER DATABANK "n" SET FILE '...'}). Once it has more than one file this property
     * disappears entirely ({@code visibleIf = SingleFileValidator}): {@code EXT_DATABANKS}'s
     * databank-level {@code FILE_NAME} isn't reliably file #1's for a multi-file databank, and
     * each file's name/sizes are managed on its own {@link MimerDatabankFile} node instead
     * ({@code ALTER DATABANK "n" ALTER FILE '...' ...}).
     */
    @Property(viewable = true, editable = true, updatableExpr = "object.fileNameEditable", visibleIf = SingleFileValidator.class, order = 6)
    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.singleFile", visibleIf = SingleFileValidator.class, order = 7)
    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.singleFile", visibleIf = SingleFileValidator.class, order = 8)
    public String getMinSize() {
        return minSize;
    }

    public void setMinSize(String minSize) {
        this.minSize = minSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.singleFile", visibleIf = SingleFileValidator.class, order = 9)
    public String getGoalSize() {
        return goalSize;
    }

    public void setGoalSize(String goalSize) {
        this.goalSize = goalSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.singleFile", visibleIf = SingleFileValidator.class, order = 10)
    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Whether this databank currently has exactly one file - used only to pick the DDL form for a
     * file edit ({@code ALTER DATABANK SET ...} vs {@code ... ALTER FILE '...' SET ...}), not for
     * property visibility. 10.1 always returns {@code true} (no multi-file databanks there).
     */
    public boolean isSingleFile() {
        return fileCount <= 1;
    }

    /**
     * One of Mimer SQL's built-in databanks ({@code SQLDB}/{@code TRANSDB}/{@code LOGDB}/{@code
     * SYSDB}). The server won't drop these or rename their files (that needs a server stop +
     * {@code bsql} re-define, out of scope here), so those actions are disabled - their
     * size limits (FILESIZE/MINSIZE/GOALSIZE/MAXSIZE) can still be changed/dropped.
     */
    public boolean isSystemDatabank() {
        return name != null && MimerConstants.SYSTEM_DATABANKS.contains(name.toUpperCase(Locale.ROOT));
    }

    /**
     * A system databank ({@link #isSystemDatabank}) gets the plain "locked" tree icon instead of
     * the tablespace one, so it reads as distinct (and protected) from a user databank without a
     * loud badge overlay. {@code null} = the tree's default {@code #tablespace} icon.
     */
    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return isSystemDatabank() ? DBIcon.TREE_LOCKED : null;
    }

    /** Whether the databank-level {@code File} name is editable (single-file, non-system). */
    public boolean isFileNameEditable() {
        return isSingleFile() && !isSystemDatabank();
    }

    /**
     * {@code visibleIf} backing for the databank-level file/size properties: they're shown here
     * only on a **pre-11.0** server, where a databank is always single-file and every file
     * operation is {@code ALTER DATABANK SET ...} on the databank itself. On 11.0+ (multi-file
     * capable) they disappear - file name/sizes are managed on the {@link MimerDatabankFile}
     * nodes instead, even for a databank that happens to have just one file
     * ({@code EXT_DATABANKS}'s databank-level {@code FILE_NAME} isn't reliably file #1's anyway).
     */
    public static class SingleFileValidator implements IPropertyValueValidator<MimerDatabank, Object> {
        @Override
        public boolean isValidValue(MimerDatabank object, Object value) {
            return !object.getDataSource().supportsMultiFileDatabanks();
        }
    }

    @Property(viewable = true, editable = true, updatable = true, order = 11)
    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON DATABANK "name" IS '...'} - a datasource-global object like Schema/
     * User/Group. See {@link org.jkiss.dbeaver.ext.mimer.edit.MimerDatabankManager} for the
     * write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 12)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, null, null, name, "DATABANK");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return dataSource;
    }

    @Association
    public Collection<MimerDatabankFile> getFiles(DBRProgressMonitor monitor) throws DBException {
        return fileCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDatabank, MimerDatabankFile> getFileCache() {
        return fileCache;
    }

    @Association
    public Collection<MimerDatabankShadow> getShadows(DBRProgressMonitor monitor) throws DBException {
        return shadowCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDatabank, MimerDatabankShadow> getShadowCache() {
        return shadowCache;
    }

    /**
     * Objects stored in this databank - tables placed here directly ({@code
     * EXT_TABLE_DATABANK_USAGE}), plus anything else the generic dependency view reports as
     * using it (there's no generic "uses" side for a databank - nothing it depends on in that
     * sense).
     */
    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    /** {@code GRANT TABLE|SEQUENCE ON DATABANK} grants - see {@link MimerDatabankPrivilege}. */
    @Association
    public Collection<MimerDatabankPrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDatabank, MimerDatabankPrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        fileCache.clearCache();
        shadowCache.clearCache();
        usedByCache.clearCache();
        privilegeCache.clearCache();
        comment = null;
        onlineTransition = MimerUtils.ONLINE_NOCHANGE;
        return this;
    }

    /**
     * Mimer SQL 10.1+ {@code CREATE DATABANK} statement, matching the clause order
     * Mimer SQL's own tooling round-trips a databank through (file, minsize, goalsize,
     * maxsize, removable, option). {@code FILE} is optional - Mimer SQL defaults it to
     * {@code "<databank name>.dbf"} server-side when omitted, so an unset {@link #file} simply
     * skips that clause rather than reconstructing the default name here.
     */
    @NotNull
    public String buildCreateDDL() {
        List<String> clauses = new ArrayList<>();
        if (!CommonUtils.isEmptyTrimmed(file)) {
            clauses.add("FILE '" + file.trim() + "'");
        }
        if (!CommonUtils.isEmptyTrimmed(fileSize)) {
            clauses.add("FILESIZE " + fileSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(minSize)) {
            clauses.add("MINSIZE " + minSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(goalSize)) {
            clauses.add("GOALSIZE " + goalSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(maxSize)) {
            clauses.add("MAXSIZE " + maxSize.trim());
        }
        if (removable) {
            clauses.add("REMOVABLE");
        }
        clauses.add("OPTION " + (CommonUtils.isEmpty(type) ? "TRANSACTION" : type));
        return "CREATE DATABANK \"" + name + "\" SET " + String.join(", ", clauses);
    }

    @NotNull
    public String buildDropDDL() {
        return "DROP DATABANK \"" + name + "\"";
    }

    /**
     * {@code ALTER DATABANK "n" SET FILE '<file>'} for a single-file databank (a multi-file one's
     * files are renamed on their own {@link MimerDatabankFile} nodes), or {@code null} when the
     * file name is blank. Its own statement (not folded into {@link #buildAlterDDL}) so the
     * manager can wrap it in the file-rename confirmation.
     */
    @Nullable
    public String buildSetFileDDL() {
        return CommonUtils.isEmptyTrimmed(file) ? null : "ALTER DATABANK \"" + name + "\" SET FILE '" + file.trim() + "'";
    }

    /**
     * {@code ALTER DATABANK ... SET ...} / {@code ... DROP ...} for the changed properties (the
     * file name is separate - see {@link #buildSetFileDDL}). A size cleared to blank drops that
     * limit; everything else is a SET. Returns up to two statements, since Mimer SQL's grammar
     * allows only one action per statement.
     */
    @NotNull
    public List<String> buildAlterDDL(@NotNull Set<String> changedProperties) {
        List<String> setClauses = new ArrayList<>();
        List<String> dropClauses = new ArrayList<>();
        if (changedProperties.contains("type") && !CommonUtils.isEmpty(type)) {
            setClauses.add("OPTION " + type);
        }
        addSizeClause(changedProperties, "fileSize", "FILESIZE", fileSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "minSize", "MINSIZE", minSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "goalSize", "GOALSIZE", goalSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "maxSize", "MAXSIZE", maxSize, setClauses, dropClauses);
        if (changedProperties.contains("removable")) {
            (removable ? setClauses : dropClauses).add("REMOVABLE");
        }

        List<String> statements = new ArrayList<>(2);
        if (!setClauses.isEmpty()) {
            statements.add("ALTER DATABANK \"" + name + "\" SET " + String.join(", ", setClauses));
        }
        if (!dropClauses.isEmpty()) {
            statements.add("ALTER DATABANK \"" + name + "\" DROP " + String.join(", ", dropClauses));
        }
        return statements;
    }

    private static void addSizeClause(
        @NotNull Set<String> changedProperties, @NotNull String propId, @NotNull String keyword,
        @Nullable String value, @NotNull List<String> setClauses, @NotNull List<String> dropClauses
    ) {
        if (!changedProperties.contains(propId)) {
            return;
        }
        if (CommonUtils.isEmptyTrimmed(value)) {
            dropClauses.add(keyword);
        } else {
            setClauses.add(keyword + " " + value.trim());
        }
    }

    public static class OnlineTransitionListProvider implements IPropertyValueListProvider<MimerDatabank> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(MimerDatabank object) {
            return MimerUtils.onlineTransitionsFor(object.isOnline());
        }
    }

    public static class OptionListProvider implements IPropertyValueListProvider<MimerDatabank> {
        // CREATE DATABANK only accepts LOG/TRANSACTION/WORK (see MimerCreateDatabankPage);
        // ALTER DATABANK SET OPTION also accepts READ ONLY, so it's only offered here.
        private static final String[] OPTIONS = {"TRANSACTION", "LOG", "WORK", "READ ONLY"};

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(MimerDatabank object) {
            return OPTIONS;
        }
    }

    static class FileCache extends JDBCObjectCache<MimerDatabank, MimerDatabankFile> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDatabank owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT FILE_NUMBER, FILE_NAME, IS_ONLINE, MINSIZE, GOALSIZE, MAXSIZE\n" +
                "FROM INFORMATION_SCHEMA.EXT_DATABANKS\n" +
                "WHERE DATABANK_NAME = ?\n" +
                "ORDER BY FILE_NUMBER");
            stmt.setString(1, owner.getName());
            return stmt;
        }

        @Override
        protected MimerDatabankFile fetchObject(@NotNull JDBCSession session, @NotNull MimerDatabank owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDatabankFile(owner, resultSet);
        }
    }

    static class ShadowCache extends JDBCObjectCache<MimerDatabank, MimerDatabankShadow> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDatabank owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT SHADOW_NAME, SHADOW_CREATOR, FILE_NAME, IS_ONLINE\n" +
                "FROM INFORMATION_SCHEMA.EXT_SHADOWS\n" +
                "WHERE DATABANK_NAME = ?\n" +
                "ORDER BY SHADOW_NAME");
            stmt.setString(1, owner.getName());
            return stmt;
        }

        @Override
        protected MimerDatabankShadow fetchObject(@NotNull JDBCSession session, @NotNull MimerDatabank owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDatabankShadow(owner, resultSet);
        }
    }

    static class UsedByCache extends JDBCObjectCache<MimerDatabank, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDatabank owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT TABLE_SCHEMA AS USING_OBJECT_SCHEMA, TABLE_NAME AS USING_OBJECT_NAME, 'TABLE' AS USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_TABLE_DATABANK_USAGE\n" +
                "WHERE DATABANK_NAME = ?\n" +
                "UNION ALL\n" +
                "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
                "WHERE USED_OBJECT_TYPE = 'DATABANK' AND USED_OBJECT_NAME = ?");
            stmt.setString(1, owner.getName());
            stmt.setString(2, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerDatabank owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }
}
