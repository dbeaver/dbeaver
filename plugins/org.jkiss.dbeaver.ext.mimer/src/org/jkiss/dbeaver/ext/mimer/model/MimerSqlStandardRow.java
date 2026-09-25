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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row of a SQL-standard-mandated {@code INFORMATION_SCHEMA} reference view ({@code
 * SQL_FEATURES}/{@code SQL_SIZING}/{@code SQL_LANGUAGES} - see {@link
 * MimerDataSource#getSqlFeatures} and friends).
 * <p>
 * These views have fixed, spec-defined column layouts, but are deliberately read generically
 * here - every column the driver actually returns, via {@code ResultSetMetaData}, none
 * hardcoded - rather than modeled with named {@code @Property} getters. It's a plain {@code
 * SELECT *} dump of static reference data, not worth hand-naming three different fixed column
 * sets for, and this way is immune to a future column addition (a lesson repeatedly learned the
 * hard way elsewhere in this plugin for catalog views that turned out to gain columns across
 * versions).
 * <p>
 * Also implements {@link DBPPropertySource}, which is what the single-row Properties
 * <i>panel</i> uses - it shows every column the {@code SELECT *} actually returned, none
 * hardcoded. The multi-row <i>grid</i> shown on double-clicking the Features/Sizing/Languages
 * folder is different: it builds its columns only from {@code @Property}-annotated getters,
 * since {@code ObjectListControl} does <b>not</b> consult {@code DBPPropertySource} for grid
 * columns (an earlier assumption here that it did was wrong, and the grid rendered every row as
 * {@code toString()} until that was fixed). So the getters below name the columns of the three
 * underlying SQL:1999/2003 spec views - a fixed, frozen set, unlike Mimer's own {@code EXT_*}
 * views - read from the generic {@code values} map and each marked {@code optional} so a grid
 * only shows the columns for the view it actually came from.
 *
 * @author Mimer Information Technology
 */
public class MimerSqlStandardRow implements DBSObject, DBPPropertySource {

    private final DBSObject owner;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final String name;

    public MimerSqlStandardRow(@NotNull DBSObject owner, @NotNull JDBCResultSet dbResult) throws SQLException {
        this.owner = owner;
        ResultSetMetaData md = dbResult.getMetaData();
        int columnCount = md.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            values.put(md.getColumnLabel(i), dbResult.getObject(i));
        }
        this.name = buildName();
    }

    /**
     * The row's first two non-blank column values, joined - for every one of the three views this
     * backs, that's already a natural identifying pair ({@code FEATURE_ID}/{@code FEATURE_NAME},
     * {@code SIZING_ID}/{@code SIZING_NAME}, {@code SQL_LANGUAGE_SOURCE}/{@code
     * SQL_LANGUAGE_YEAR}) without needing to know which view produced the row.
     */
    @NotNull
    private String buildName() {
        List<String> parts = new ArrayList<>(2);
        for (Object value : values.values()) {
            String text = CommonUtils.toString(value, "");
            if (!text.isEmpty()) {
                parts.add(text);
                if (parts.size() == 2) {
                    break;
                }
            }
        }
        return parts.isEmpty() ? "Row" : String.join(" ", parts);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, name = "Name")
    public String getName() {
        return name;
    }

    // Columns of INFORMATION_SCHEMA.SQL_FEATURES / SQL_SIZING / SQL_LANGUAGES - see the class
    // Javadoc. Read from the generic values map (case-insensitive), all optional so each grid
    // only shows its own view's columns; the Properties panel still shows the full raw column
    // set via DBPPropertySource below.

    @Nullable
    @Property(viewable = true, optional = true, order = 10, name = "Feature ID")
    public String getFeatureId() {
        return get("FEATURE_ID");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 11, name = "Feature name")
    public String getFeatureName() {
        return get("FEATURE_NAME");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 12, name = "Sub-feature ID")
    public String getSubFeatureId() {
        return get("SUB_FEATURE_ID");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 13, name = "Sub-feature name")
    public String getSubFeatureName() {
        return get("SUB_FEATURE_NAME");
    }

    /**
     * {@code IS_SUPPORTED} as a {@code Boolean} - {@code null} for the Sizing/Languages views
     * (which don't have this column, so the "optional" column stays hidden there); for Features
     * it renders with DBeaver's built-in boolean style (green check / red cross) so supported vs
     * not-supported is visible at a glance. Per-row background colouring isn't available for this
     * grid.
     */
    @Nullable
    @Property(viewable = true, optional = true, order = 14, name = "Supported")
    public Boolean getSupported() {
        String value = get("IS_SUPPORTED");
        return value == null ? null : "YES".equalsIgnoreCase(value);
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 20, name = "Sizing ID")
    public String getSizingId() {
        return get("SIZING_ID");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 21, name = "Sizing name")
    public String getSizingName() {
        return get("SIZING_NAME");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 22, name = "Supported value")
    public String getSupportedValue() {
        return get("SUPPORTED_VALUE");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 30, name = "Language source")
    public String getLanguageSource() {
        return get("SQL_LANGUAGE_SOURCE");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 31, name = "Year")
    public String getLanguageYear() {
        return get("SQL_LANGUAGE_YEAR");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 32, name = "Conformance")
    public String getLanguageConformance() {
        return get("SQL_LANGUAGE_CONFORMANCE");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 33, name = "Binding style")
    public String getLanguageBindingStyle() {
        return get("SQL_LANGUAGE_BINDING_STYLE");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 34, name = "Programming language")
    public String getLanguageProgrammingLanguage() {
        return get("SQL_LANGUAGE_PROGRAMMING_LANGUAGE");
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 90, name = "Comments")
    public String getComments() {
        return get("COMMENTS");
    }

    /**
     * Case-insensitive lookup into {@link #values} - the driver's own {@code ResultSetMetaData}
     * column labels drive the map keys, and their case isn't guaranteed.
     */
    @Nullable
    private String get(@NotNull String column) {
        Object value = values.get(column);
        if (value == null) {
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (e.getKey().equalsIgnoreCase(column)) {
                    value = e.getValue();
                    break;
                }
            }
        }
        return value == null ? null : CommonUtils.toString(value);
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) owner.getDataSource();
    }

    @NotNull
    @Override
    public Object getEditableValue() {
        return this;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getProperties() {
        DBPPropertyDescriptor[] result = new DBPPropertyDescriptor[values.size()];
        int i = 0;
        for (String column : values.keySet()) {
            result[i++] = new PropertyDescriptor("SQL Standard", column, column, null, null, false, null, null, false);
        }
        return result;
    }

    @Nullable
    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id) {
        return values.get(id);
    }

    @Override
    public boolean isPropertySet(@NotNull String id) {
        return values.get(id) != null;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id) {
        // read-only
    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id, @Nullable Object value) {
        // read-only
    }

    @Override
    public boolean isPropertyResettable(@NotNull String id) {
        return false;
    }

    @Override
    public void resetPropertyValueToDefault(@NotNull String id) {
        // read-only
    }
}
