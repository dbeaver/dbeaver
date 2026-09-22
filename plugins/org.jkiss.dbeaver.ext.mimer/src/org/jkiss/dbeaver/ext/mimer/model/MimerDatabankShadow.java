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
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.List;

/**
 * One shadow of a Mimer SQL databank - a continuously-updated replica used for backup/failover,
 * read from {@code INFORMATION_SCHEMA.EXT_SHADOWS} scoped to the owning databank. Shown per
 * databank (this class) as well as flatly across every databank under DBA Views (the older,
 * read-only {@link MimerShadow}) - two separate classes over the same catalog view, same
 * "flat read-only summary vs. richer per-owner object" split already used for
 * {@link MimerSchemaIndex} vs. {@link MimerTableIndex}.
 * <p>
 * {@code ALTER SHADOW} has three forms, all supported: {@code INTO '<file>'} (the editable
 * {@link #getFileName() file name} property), {@code ADD ... PAGES} (the "Add Pages" navigator
 * action) and {@code TO MASTER} (the "Switch To Master" navigator action). Online/offline is a
 * separate {@code SET SHADOW} family, via the {@link #getOnlineTransition() "Change state"}
 * dropdown or the "Set Online State" navigator action. Does not implement {@code DBPNamedObject2}
 * - that would expose the navigator's inline "Rename" gesture on an already-persisted shadow,
 * which nothing here backs with a rename statement; {@link #setName} exists only for the create flow.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankShadow implements DBSObject, DBPSaveableObject {

    private final MimerDatabank databank;
    private String name;
    private String creator;
    private String fileName;
    private boolean online;
    private String onlineTransition = MimerUtils.ONLINE_NOCHANGE;
    private boolean persisted;

    public MimerDatabankShadow(@NotNull MimerDatabank databank, @NotNull JDBCResultSet dbResult) {
        this.databank = databank;
        this.name = JDBCUtils.safeGetString(dbResult, "SHADOW_NAME");
        this.creator = JDBCUtils.safeGetString(dbResult, "SHADOW_CREATOR");
        this.fileName = JDBCUtils.safeGetString(dbResult, "FILE_NAME");
        this.online = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_ONLINE"));
        this.persisted = true;
    }

    public MimerDatabankShadow(@NotNull MimerDatabank databank, @NotNull String name) {
        this.databank = databank;
        this.name = name;
        this.online = true;
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

    /**
     * The shadow's file. Editable for a persisted shadow - a change emits
     * {@code ALTER SHADOW "n" INTO '<file>'} (see {@link #buildAlterFileNameDDL}), Mimer SQL's
     * way of pointing the data dictionary at a moved/renamed shadow file.
     */
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Property(viewable = true, order = 3)
    public String getCreator() {
        return creator;
    }

    /**
     * The shadow's real, catalog-persisted online/offline state ({@code EXT_SHADOWS.IS_ONLINE})
     * - a read-only checkbox. Same treatment as {@link MimerDatabank#isOnline()}.
     */
    @Property(viewable = true, order = 4)
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * "Change state" action dropdown - {@link MimerUtils#ONLINE_NOCHANGE} (no-op) by default,
     * options gated on the current {@link #isOnline() state}. Same treatment as
     * {@link MimerDatabank#getOnlineTransition()}, keyword {@code SHADOW}.
     */
    @Property(viewable = true, editable = true, updatable = true, order = 5, listProvider = OnlineTransitionListProvider.class)
    public String getOnlineTransition() {
        return onlineTransition;
    }

    public void setOnlineTransition(String onlineTransition) {
        this.onlineTransition = onlineTransition;
    }

    @Nullable
    public String buildSetOnlineDDL() {
        return MimerUtils.buildSetOnlineDDL("SHADOW", List.of(name), onlineTransition);
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
     * The file(s) backing this shadow - lets the shadow node be expanded in the navigator to see
     * which physical file it uses. Synthesised from the loaded {@link #getFileName() file name}
     * (single-file, matching DbVisualizer's model) - see {@link MimerShadowFile}.
     */
    @Association
    public Collection<MimerShadowFile> getFiles(@NotNull DBRProgressMonitor monitor) {
        return List.of(new MimerShadowFile(this, fileName, online));
    }

    /**
     * {@code CREATE SHADOW "name" FOR "databank" IN 'filename'} - unlike a databank's own FILE
     * clause, the file name is mandatory here, not optional.
     */
    @NotNull
    public String buildCreateDDL() {
        return "CREATE SHADOW \"" + name + "\" FOR \"" + databank.getName() + "\" IN '" + fileName + "'";
    }

    @NotNull
    public String buildDropDDL() {
        return "DROP SHADOW \"" + name + "\"";
    }

    /**
     * {@code ALTER SHADOW "n" INTO '<file>'} for a changed {@link #getFileName() file name}, or
     * {@code null} when the file name is blank (nothing to point at).
     */
    @Nullable
    public String buildAlterFileNameDDL() {
        return fileName == null || fileName.isBlank()
            ? null
            : "ALTER SHADOW \"" + name + "\" INTO '" + fileName.trim() + "'";
    }

    /** {@code ALTER SHADOW "n" ADD <pages> PAGES} - extend the shadow file (2K Mimer SQL pages). */
    @NotNull
    public static String buildAddPagesDDL(@NotNull String shadowName, int pages) {
        return "ALTER SHADOW \"" + shadowName + "\" ADD " + pages + " PAGES";
    }

    public static class OnlineTransitionListProvider implements IPropertyValueListProvider<MimerDatabankShadow> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(MimerDatabankShadow object) {
            return MimerUtils.onlineTransitionsFor(object.isOnline());
        }
    }
}
