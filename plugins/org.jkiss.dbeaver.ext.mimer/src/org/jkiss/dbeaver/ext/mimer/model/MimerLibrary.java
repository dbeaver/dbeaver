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
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Collection;

/**
 * An external library ({@code CREATE LIBRARY "n" FILE 'path' LANGUAGE CLR}). This exposes the
 * procedures/functions defined in an external file - a .NET DLL today, other languages besides
 * CLR may follow in a future server version - for use as a procedure/function body via {@code
 * LANGUAGE CLR EXTERNAL NAME '...' IN "n"} (see {@link MimerProcedure}/{@link
 * org.jkiss.dbeaver.ext.mimer.model.MimerMetaModel#getProcedureDDL}).
 * <p>
 * Read from {@code INFORMATION_SCHEMA.EXT_LIBRARIES} - a datasource-global object, no schema
 * (confirmed both by that view's own columns, which carry no schema, and {@code CREATE
 * LIBRARY}'s own grammar, which never qualifies {@code library-name}). Mimer SQL 11.1+ only.
 * <p>
 * No {@code ALTER LIBRARY} exists - a library is create/drop only, matching e.g. {@link
 * MimerSequence}'s simplest (10.1) form or {@link MimerCollation} before its Comment support.
 * Implements {@link DBPSaveableObject} itself since a plain leaf {@code DBSObject} has no
 * persisted-state tracking.
 * <p>
 * {@link #getUsedBy} (the procedures/functions implemented in this library) is deliberately a
 * custom query directly against {@code ROUTINES.EXTERNAL_LIBRARY}, not the generic {@code
 * EXT_OBJECT_OBJECT_USED} mechanism {@link MimerObjectUsedBy} otherwise reads from (see the
 * "Used By"/"Uses" feature elsewhere in this plugin). External libraries are an 11.1+ feature.
 * @author Mimer Information Technology
 */
public class MimerLibrary implements DBSObject, DBPSaveableObject {

    private final MimerDataSource dataSource;
    private String name;
    private final String creator;
    private String language;
    private String fileName;
    private boolean persisted = true;
    private final UsedByCache usedByCache = new UsedByCache();

    public MimerLibrary(@NotNull MimerDataSource dataSource, @NotNull JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "LIBRARY_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "LIBRARY_CREATOR");
        this.language = JDBCUtils.safeGetStringTrimmed(dbResult, "LIBRARY_LANGUAGE");
        this.fileName = JDBCUtils.safeGetString(dbResult, "LIBRARY_FILENAME");
    }

    /**
     * For a brand-new, not-yet-created library - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerLibraryManager}.
     */
    public MimerLibrary(@NotNull MimerDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
        this.creator = null;
        this.language = "CLR";
        this.fileName = "";
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 2)
    public String getCreator() {
        return creator;
    }

    /**
     * A free-form value on the server ({@code CLR} today - see the class Javadoc), so kept as a
     * plain string here rather than a fixed enum.
     */
    @Property(viewable = true, order = 3)
    public String getLanguage() {
        return language;
    }

    public void setLanguage(@NotNull String language) {
        this.language = language;
    }

    /**
     * The absolute file path to the library file - must already exist on the server before
     * {@code CREATE LIBRARY} is run (the statement doesn't create it).
     */
    @Property(viewable = true, order = 4)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@NotNull String fileName) {
        this.fileName = fileName;
    }

    /**
     * {@code CREATE LIBRARY "name" FILE 'path' LANGUAGE <language>}.
     */
    @NotNull
    public String buildCreateDDL() {
        return "CREATE LIBRARY \"" + name + "\" FILE '" + fileName.replace("'", "''") + "' LANGUAGE " + language;
    }

    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
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
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Every procedure/function implemented in this library, across every schema (a library isn't
     * schema-scoped, so neither is what uses it).
     */
    static class UsedByCache extends JDBCObjectCache<MimerLibrary, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerLibrary owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT ROUTINE_SCHEMA AS USING_OBJECT_SCHEMA, ROUTINE_NAME AS USING_OBJECT_NAME,\n" +
                "       ROUTINE_TYPE AS USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE EXTERNAL_LIBRARY = ?\n" +
                "ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME");
            stmt.setString(1, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerLibrary owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }
}
