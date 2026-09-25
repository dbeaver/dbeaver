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
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code GenericDataTypeCache} plus a few types the driver doesn't report on its own.
 * <p>
 * Mimer SQL's {@code INTERVAL} type family never comes back from the driver's own {@code
 * getTypeInfo()} - there's no single fixed "INTERVAL" type, just a family of qualifiers - so
 * without help it would never appear in the Data Type picker at all. {@link #addCustomObjects}
 * (the standard {@code AbstractObjectCache} extension point for exactly this) adds it instead of
 * trying to make the driver report it. {@code Types.OTHER} is fine as the JDBC type code here,
 * since {@link MimerDataSource#resolveDataKind} classifies these by name prefix as {@code
 * DATETIME} regardless of the code.
 * <p>
 * Mimer SQL's three LOB types ({@link MimerConstants#TYPE_NAMES_LARGE_OBJECT}) are added the
 * same way, but registered with their real {@code java.sql.Types} codes - unlike {@code
 * INTERVAL}, these do have one. That means {@code getDataKind()} for a column created via this
 * picker resolves correctly without even needing {@link MimerDataSource#resolveDataKind}'s
 * by-name override; that override is still needed to fix an *existing* column's tree icon,
 * since reading an existing column goes through the driver's own (apparently wrong) reported
 * code instead of this cache. Being a real, cached entry still isn't enough on its own to get a
 * user-set Length appended to the DDL for one of these - see {@code
 * MimerSQLDialect#getColumnTypeModifiers} for that separate fix.
 * <p>
 * {@code BINARY LARGE OBJECT}/{@code CHARACTER LARGE OBJECT} are genuinely absent from the
 * driver's own {@code getTypeInfo()} (true of all three LOB types), so {@link #fetchObject}'s
 * dedup never sees them and {@link #addCustomObjects} always adds them. {@code NATIONAL CHAR
 * LARGE OBJECT} is fussier: the driver does report this exact name, just with a {@code
 * DATA_TYPE} code {@code java.sql.Types} has no match for, so its resolved kind comes back
 * {@code UNKNOWN}. That's useless as a picker entry on its own ({@code
 * ColumnTypeNameListProvider} filters out any {@code UNKNOWN}-kind type) - and worse, letting it
 * register as "seen" the normal way would make {@code addCustomObjects} wrongly skip adding our
 * own correctly-kinded replacement for the same name. So {@link #fetchObject} now drops any
 * driver-reported row for one of these three names outright when its own kind resolves {@code
 * UNKNOWN}, rather than marking it seen, so {@link #addCustomObjects} always gets to supply the
 * real one.
 * <p>
 * The short abbreviations ({@link MimerConstants#TYPE_NAMES_LARGE_OBJECT_ABBREVIATED} - {@code
 * BLOB}/{@code CLOB}/{@code NCLOB}/{@code CHAR LARGE OBJECT}/{@code NCHAR LARGE OBJECT}) are
 * deliberately not added as their own picker entries. Every one of them is echoed back as its
 * spelled-out form once the column is saved and the tree refreshes, so listing both forms would
 * just be six near-duplicate entries collapsing down to the same three real types - and
 * DbVisualizer's own picker, the reference for this plugin, only ever lists the three
 * spelled-out names. Typing an abbreviation directly still works correctly - {@link
 * MimerDataSource#resolveDataKind} and {@code MimerSQLDialect#getColumnTypeModifiers} both still
 * recognize the abbreviations by name, this only trims what the dropdown lists, not what's
 * accepted.
 * <p>
 * Duplicate driver-reported type names (Mimer SQL's {@code getTypeInfo()} reports {@code
 * NUMERIC}/{@code INTEGER} more than once) are also dropped here rather than left to {@code
 * AbstractObjectCache}'s own duplicate handling - that still works (picks one via {@link
 * #isValidDuplicateObject}), but it always logs a debug line too, which a subclass can't opt
 * out of. Keeping only the first occurrence is fine for these common, unambiguous types -
 * whichever row the driver reports first already has a real kind and correct properties.
 * <p>
 * Finally, the list is sorted alphabetically by name - the driver reports types in no
 * particular order, which would otherwise carry straight through to both the datasource-level
 * "Data Types" node and any column-creation type picker built from this cache.
 *
 * @author Mimer Information Technology
 */
public class MimerDataTypeCache extends GenericDataTypeCache {

    private final Set<String> seenTypeNames = new HashSet<>();

    public MimerDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    public void beforeCacheLoading(@NotNull JDBCSession session, GenericStructContainer owner) throws DBException {
        super.beforeCacheLoading(session, owner);
        seenTypeNames.clear();
    }

    @Nullable
    @Override
    protected GenericDataType fetchObject(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull JDBCResultSet dbResult
    ) throws SQLException, DBException {
        GenericDataType dataType = super.fetchObject(session, owner, dbResult);
        if (dataType == null) {
            return null;
        }
        String upperName = dataType.getName().toUpperCase(Locale.ENGLISH);
        if (isKnownLargeObjectName(upperName) && dataType.getDataKind() == DBPDataKind.UNKNOWN) {
            // The driver DOES report this exact name (unlike BINARY/CHARACTER LARGE OBJECT,
            // which per addCustomObjects' own comment aren't reported at all) - but with a
            // DATA_TYPE code java.sql.Types has no match for, so its resolved kind comes back
            // UNKNOWN. Left as-is, that's a double problem: it's useless as a picker entry on
            // its own (ColumnTypeNameListProvider filters out any UNKNOWN-kind type), AND
            // marking it "seen" here would wrongly stop addCustomObjects from adding our own
            // correctly-kinded replacement for the same name. Drop it entirely - don't cache
            // it, don't mark it seen - so addCustomObjects's own entry takes over
            // cleanly instead.
            return null;
        }
        if (!seenTypeNames.add(upperName)) {
            return null;
        }
        return dataType;
    }

    private static boolean isKnownLargeObjectName(@NotNull String upperName) {
        return MimerConstants.TYPE_NAMES_LARGE_OBJECT.contains(upperName)
            || MimerConstants.TYPE_NAMES_LARGE_OBJECT_ABBREVIATED.contains(upperName);
    }

    @Override
    protected void addCustomObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericStructContainer owner,
        @NotNull List<GenericDataType> objectList
    ) throws DBException {
        for (String typeName : MimerConstants.INTERVAL_TYPE_NAMES) {
            objectList.add(new GenericDataType(owner, Types.OTHER, typeName, null, false, true, 0, 0, 0));
        }
        for (String typeName : MimerConstants.TYPE_NAMES_LARGE_OBJECT) {
            if (seenTypeNames.contains(typeName.toUpperCase(Locale.ENGLISH))) {
                // Already reported correctly by the driver itself - only NATIONAL CHARACTER
                // LARGE OBJECT was actually confirmed missing live, the other two are added
                // proactively (see the class javadoc) and skipped here if not actually needed.
                continue;
            }
            objectList.add(new GenericDataType(owner, largeObjectTypeCode(typeName), typeName, null, false, true, 0, 0, 0));
        }
        // Deliberately not also adding MimerConstants.TYPE_NAMES_LARGE_OBJECT_ABBREVIATED here -
        // see the class javadoc for why (redundant with the three above once persisted, and not
        // what DbVisualizer's own picker offers either).
        objectList.sort(Comparator.comparing(GenericDataType::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private static int largeObjectTypeCode(@NotNull String typeName) {
        return switch (typeName) {
            case "BINARY LARGE OBJECT" -> Types.BLOB;
            case "NATIONAL CHAR LARGE OBJECT" -> Types.NCLOB;
            default -> Types.CLOB;
        };
    }
}
