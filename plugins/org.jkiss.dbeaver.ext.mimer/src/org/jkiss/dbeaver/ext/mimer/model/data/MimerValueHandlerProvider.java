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
package org.jkiss.dbeaver.ext.mimer.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;
import java.util.Locale;

/**
 * Registered in {@code plugin.xml} ({@code org.jkiss.dbeaver.dataTypeProvider}, scoped to
 * {@code <datasource id="mimer"/>} plus one {@code <type id="...">}/{@code <type name="...">}
 * per type handled here) so {@link MimerBLOBValueHandler}/{@link MimerCLOBValueHandler} - the fix
 * for a Mimer SQL JDBC Driver limitation reading {@code BLOB}/{@code CLOB}/{@code NCLOB} values,
 * see their own javadoc - are used automatically for every matching column on a Mimer SQL
 * connection. A new case here needs a matching {@code <type>} entry in {@code plugin.xml} too, or
 * the registry filters the column out before this class is ever consulted.
 * <p>
 * {@link #getValueHandler} tries {@code typedObject.getTypeID()} first, but the driver's raw
 * catalog {@code DATA_TYPE} for {@code NATIONAL CHAR LARGE OBJECT} doesn't match {@code
 * Types.NCLOB} (see {@link MimerConstants#TYPE_NAMES_LARGE_OBJECT}'s own comment) -
 * {@code GenericMetaModel#createTableColumnImpl}/{@code JDBCColumnMetaData} normally correct it
 * via {@code MimerDataSource#getLocalDataType(String)} before this class ever sees it, but that's
 * an indirect side effect of a type-name lookup elsewhere in this plugin, not something this class
 * controls. So it also falls back to matching {@code typedObject.getTypeName()} directly against
 * the same name sets {@code MimerDataSource#resolveDataKind} already uses, rather than trusting
 * the type ID alone.
 * <p>
 * Falls back to {@link JDBCContentValueHandler#INSTANCE} - the same handler the core JDBC-standard
 * provider would give a BLOB/CLOB/NCLOB column - once {@link
 * MimerDataSource#isDriverVersionAtLeast(int, int, int)} says the connected driver is 11.0.6 or
 * later, but only when the type ID itself already matched BLOB/CLOB/NCLOB directly: that cutoff
 * is for the separate {@code getObject()}/stream-read bug, not the vendor-ID mismatch the name
 * fallback above exists for, so a column only reached via the name fallback keeps using the
 * Mimer-specific wrapper regardless of driver version - its null/create-new handling still can't
 * be trusted to the standard handler while the ID itself stays wrong. Returning {@code null} here
 * would NOT reach that standard provider: {@code DataTypeProviderRegistry#getValueHandlerProvider}
 * picks this, the first non-global provider matching both the datasource and the column type, and
 * returns immediately - the global providers are never even consulted - so
 * {@code DBUtils#findValueHandler} would fall straight through to {@code DefaultValueHandler},
 * which can't bind a value at all (throws on every write) and skips the content-wrapper machinery
 * LOB viewing/export relies on. 11.0.6 isn't released yet and isn't confirmed to actually carry
 * the fix - bump the patch number here if it turns out not to.
 *
 * @author Mimer Information Technology
 */
public class MimerValueHandlerProvider implements DBDValueHandlerProvider {

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDFormatSettings preferences,
        @NotNull DBSTypedObject typedObject
    ) {
        Boolean isBlobById = classifyByTypeId(typedObject.getTypeID());
        if (isBlobById != null) {
            boolean fixedByDriver = dataSource instanceof MimerDataSource mimerDataSource
                && mimerDataSource.isDriverVersionAtLeast(11, 0, 6);
            if (fixedByDriver) {
                return JDBCContentValueHandler.INSTANCE;
            }
            return isBlobById ? MimerBLOBValueHandler.INSTANCE : MimerCLOBValueHandler.INSTANCE;
        }
        // The type ID itself didn't match BLOB/CLOB/NCLOB, so this connection's vendor-ID quirk
        // (see this class's own javadoc) is still present regardless of isDriverVersionAtLeast -
        // that cutoff only covers the separate getObject()/stream-read bug, not this ID mismatch,
        // so the Mimer-specific wrapper (and its own null/create-new handling) always applies here.
        Boolean isBlobByName = classifyByName(typedObject.getTypeName());
        if (isBlobByName == null) {
            return null;
        }
        return isBlobByName ? MimerBLOBValueHandler.INSTANCE : MimerCLOBValueHandler.INSTANCE;
    }

    /**
     * {@code true} for a BLOB-like type, {@code false} for a CLOB/NCLOB-like type, {@code null}
     * if {@code typeId} isn't one of Mimer SQL's three LOB types at all.
     */
    @Nullable
    private static Boolean classifyByTypeId(int typeId) {
        switch (typeId) {
            case Types.BLOB:
                return true;
            case Types.CLOB:
            case Types.NCLOB:
                return false;
            default:
                return null;
        }
    }

    /**
     * Same classification as {@link #classifyByTypeId(int)}, by name instead of type ID.
     */
    @Nullable
    private static Boolean classifyByName(@Nullable String typeName) {
        if (typeName == null) {
            return null;
        }
        String upperTypeName = typeName.toUpperCase(Locale.ENGLISH);
        if (!MimerConstants.TYPE_NAMES_LARGE_OBJECT.contains(upperTypeName)
            && !MimerConstants.TYPE_NAMES_LARGE_OBJECT_ABBREVIATED.contains(upperTypeName)) {
            return null;
        }
        return upperTypeName.equals("BLOB") || upperTypeName.contains("BINARY");
    }
}
