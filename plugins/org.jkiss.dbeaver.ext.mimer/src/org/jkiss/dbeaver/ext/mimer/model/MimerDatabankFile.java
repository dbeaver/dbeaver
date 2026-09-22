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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One file of a Mimer SQL databank. Every databank has at least one (its FILE_NUMBER 1
 * row in {@code INFORMATION_SCHEMA.EXT_DATABANKS}); a multi-file databank has one row
 * per extra file added with {@code ALTER DATABANK ... ADD FILE} (those extra rows carry
 * {@code DATABANK_TYPE = 'PART'}).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankFile implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    private final MimerDatabank databank;
    private String fileName;
    // The name as last loaded/persisted - the target of ALTER FILE '<here>' SET FILE '<new>'
    // when the grid edit renames a multi-file databank's file. Resynced on a successful rename
    // so a second edit on the same in-memory object still points at the right file.
    private String loadedFileName;
    private int fileNumber;
    private boolean online;
    private boolean persisted;

    // Sizes. minSize/goalSize/maxSize ARE re-hydrated from EXT_DATABANKS; fileSize is not
    // (no "current size" column - shows blank for an existing file, set-only). Editable on any
    // 11.0+ server (updatableExpr "object.fileEditable"); on 10.1 the databank's own file/size
    // properties are used instead. See #buildAlterFileDDL / #buildAddFileDDL.
    private String fileSize;
    private String minSize;
    private String goalSize;
    private String maxSize;

    public MimerDatabankFile(@NotNull MimerDatabank databank, @NotNull JDBCResultSet dbResult) {
        this.databank = databank;
        this.fileName = JDBCUtils.safeGetString(dbResult, "FILE_NAME");
        this.loadedFileName = this.fileName;
        this.fileNumber = JDBCUtils.safeGetInt(dbResult, "FILE_NUMBER");
        this.online = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_ONLINE"));
        this.minSize = formatSize(JDBCUtils.safeGetLongNullable(dbResult, "MINSIZE"));
        this.goalSize = formatSize(JDBCUtils.safeGetLongNullable(dbResult, "GOALSIZE"));
        this.maxSize = formatSize(JDBCUtils.safeGetLongNullable(dbResult, "MAXSIZE"));
        this.persisted = true;
    }

    public MimerDatabankFile(@NotNull MimerDatabank databank, @NotNull String fileName) {
        this.databank = databank;
        this.fileName = fileName;
        this.loadedFileName = fileName;
        this.online = true;
        this.persisted = false;
    }

    @Nullable
    static String formatSize(@Nullable Number kilobytes) {
        return kilobytes == null ? null : kilobytes + "K";
    }

    /**
     * The file name. Editable on any 11.0+ server (except a system databank) → the
     * {@code hasProperty("fileName")} branch of {@code MimerDatabankFileManager.addObjectModifyActions}
     * → {@link #buildRenameFileDDL} ({@code ALTER DATABANK "db" ALTER FILE '<old>' SET FILE
     * '<new>'}, or the file-less {@code SET FILE} form for a single-file databank). Only updates the
     * data dictionary - the file on the server must already have been moved (the manager confirms).
     * <p>
     * Explicit {@code id = "fileName"} (not the getter-derived {@code "name"}): core's {@code
     * CustomFormEditor} force-enables any property whose id is {@code DBConstants.PROP_ID_NAME}
     * when the manager is a {@code DBEObjectRenamer}, ignoring {@code updatableExpr} - which would
     * leave a system databank's file name editable in the file's own properties editor. As a plain
     * {@code "fileName"} property it's read-only there exactly as in the list grid.
     */
    @NotNull
    @Override
    @Property(id = "fileName", viewable = true, editable = true, updatableExpr = "object.fileNameEditable", order = 1)
    public String getName() {
        return fileName;
    }

    @Override
    public void setName(String name) {
        this.fileName = name;
    }

    @Property(viewable = true, order = 2)
    public int getFileNumber() {
        return fileNumber;
    }

    @Property(viewable = true, order = 3)
    public boolean isOnline() {
        return online;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.fileEditable", order = 4)
    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.fileEditable", order = 5)
    public String getMinSize() {
        return minSize;
    }

    public void setMinSize(String minSize) {
        this.minSize = minSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.fileEditable", order = 6)
    public String getGoalSize() {
        return goalSize;
    }

    public void setGoalSize(String goalSize) {
        this.goalSize = goalSize;
    }

    @Property(viewable = true, editable = true, updatableExpr = "object.fileEditable", order = 7)
    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
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
        return databank;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return databank.getDataSource();
    }

    @NotNull
    public MimerDatabank getDatabank() {
        return databank;
    }

    /**
     * Whether this file's sizes are editable here (grid + own editor). True on any 11.0+ server
     * (`ALTER DATABANK ... ALTER FILE`, or the file-less form for a single-file databank); false
     * on 10.1, where the databank's own file/size properties are used instead.
     */
    public boolean isFileEditable() {
        return databank.getDataSource().supportsMultiFileDatabanks();
    }

    /**
     * Whether the file <i>name</i> can be changed here - {@link #isFileEditable() editable} and
     * not a built-in system databank ({@code SQLDB}/{@code TRANSDB}/{@code LOGDB}/{@code SYSDB}),
     * whose files can only be relocated by stopping the server and re-defining them in {@code
     * bsql}. Their size limits are still editable.
     */
    public boolean isFileNameEditable() {
        return isFileEditable() && !databank.isSystemDatabank();
    }

    /**
     * {@code ALTER DATABANK ... ADD FILE} - one file per statement (Mimer SQL only
     * allows one ADD/DROP FILE operation in flight per databank at a time).
     */
    @NotNull
    public String buildAddFileDDL() {
        StringBuilder sb = new StringBuilder("ALTER DATABANK \"");
        sb.append(databank.getName()).append("\" ADD FILE '").append(fileName).append('\'');
        if (!CommonUtils.isEmptyTrimmed(fileSize)) {
            sb.append(", FILESIZE ").append(fileSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(minSize)) {
            sb.append(", MINSIZE ").append(minSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(goalSize)) {
            sb.append(", GOALSIZE ").append(goalSize.trim());
        }
        if (!CommonUtils.isEmptyTrimmed(maxSize)) {
            sb.append(", MAXSIZE ").append(maxSize.trim());
        }
        return sb.toString();
    }

    @NotNull
    public String buildDropFileDDL() {
        return "ALTER DATABANK \"" + databank.getName() + "\" DROP FILE '" + fileName + "'";
    }

    /**
     * DDL for the changed size properties: for a multi-file databank {@code ALTER DATABANK "db"
     * ALTER FILE '<file>' SET/DROP <option>} (the form Mimer SQL requires once there's more than
     * one file); for a single-file databank the file-less {@code ALTER DATABANK "db" SET/DROP
     * <option>}. A size cleared to blank drops that limit; everything else is a SET. Up to two
     * statements, since only one SET or DROP is allowed per statement. 11.0+ only (on 10.1 these
     * properties aren't editable here - the databank's own are used instead).
     */
    @NotNull
    public List<String> buildAlterFileDDL(@NotNull Set<String> changedProperties) {
        List<String> setClauses = new ArrayList<>();
        List<String> dropClauses = new ArrayList<>();
        addSizeClause(changedProperties, "fileSize", "FILESIZE", fileSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "minSize", "MINSIZE", minSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "goalSize", "GOALSIZE", goalSize, setClauses, dropClauses);
        addSizeClause(changedProperties, "maxSize", "MAXSIZE", maxSize, setClauses, dropClauses);

        String prefix = "ALTER DATABANK \"" + databank.getName() + "\" "
            + (databank.isSingleFile() ? "" : "ALTER FILE '" + fileName + "' ");
        List<String> statements = new ArrayList<>(2);
        if (!setClauses.isEmpty()) {
            statements.add(prefix + "SET " + String.join(", ", setClauses));
        }
        if (!dropClauses.isEmpty()) {
            statements.add(prefix + "DROP " + String.join(", ", dropClauses));
        }
        return statements;
    }

    /**
     * File-rename DDL with explicit names (used by the rename-command path - properties editor /
     * F2 - where both names come from the command): for a multi-file databank
     * {@code ALTER DATABANK "db" ALTER FILE '<old>' SET FILE '<new>'}; for a single-file one the
     * file-less {@code ALTER DATABANK "db" SET FILE '<new>'} form. Data-dictionary only - the
     * manager wraps it in the "rename the file on disk first" confirmation.
     */
    @NotNull
    public String buildRenameFileDDL(@NotNull String oldFileName, @NotNull String newFileName) {
        String db = "ALTER DATABANK \"" + databank.getName() + "\" ";
        return databank.isSingleFile()
            ? db + "SET FILE '" + newFileName.trim() + "'"
            : db + "ALTER FILE '" + oldFileName + "' SET FILE '" + newFileName.trim() + "'";
    }

    /**
     * File-rename DDL for the property-grid edit - the loaded name is the {@code ALTER FILE}
     * target - or {@code null} if the name is blank / unchanged.
     */
    @Nullable
    public String buildRenameFileDDL() {
        if (CommonUtils.isEmptyTrimmed(fileName) || fileName.equals(loadedFileName)) {
            return null;
        }
        return buildRenameFileDDL(CommonUtils.isEmptyTrimmed(loadedFileName) ? fileName : loadedFileName, fileName);
    }

    /** The last-loaded name, for the {@code ALTER FILE '<here>'} target and the confirm dialog. */
    @Nullable
    public String getLoadedFileName() {
        return loadedFileName;
    }

    /** Resync the loaded name after a successful rename (so a second edit targets the new file). */
    public void resyncLoadedFileName() {
        this.loadedFileName = this.fileName;
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
}
