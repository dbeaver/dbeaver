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
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Registered in {@code plugin.xml} ({@code org.jkiss.dbeaver.dataTypeProvider}, scoped to
 * {@code <datasource id="mimer"/>} plus one {@code <type id="...">} per type handled here) so
 * {@link MimerBLOBValueHandler}/{@link MimerCLOBValueHandler} - the fix for a Mimer SQL JDBC
 * Driver limitation reading {@code BLOB}/{@code CLOB}/{@code NCLOB} values, see their own javadoc
 * - are used automatically for every matching column on a Mimer SQL connection. A new case here
 * needs a matching {@code <type>} entry in {@code plugin.xml} too, or the registry filters the
 * column out before this class is ever consulted.
 * <p>
 * Falls back to {@link JDBCContentValueHandler#INSTANCE} - the same handler the core JDBC-standard
 * provider would give a BLOB/CLOB/NCLOB column - once {@link
 * MimerDataSource#isDriverVersionAtLeast(int, int, int)} says the connected driver is 11.0.6 or
 * later. Returning {@code null} here would NOT reach that standard provider:
 * {@code DataTypeProviderRegistry#getValueHandlerProvider} picks this, the first non-global
 * provider matching both the datasource and the column type, and returns immediately - the global
 * providers are never even consulted - so {@code DBUtils#findValueHandler} would fall straight
 * through to {@code DefaultValueHandler}, which can't bind a value at all (throws on every write)
 * and skips the content-wrapper machinery LOB viewing/export relies on. 11.0.6 isn't released yet
 * and isn't confirmed to actually carry the fix - bump the patch number here if it turns out not
 * to.
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
        boolean fixedByDriver = dataSource instanceof MimerDataSource mimerDataSource
            && mimerDataSource.isDriverVersionAtLeast(11, 0, 6);
        switch (typedObject.getTypeID()) {
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                if (fixedByDriver) {
                    return JDBCContentValueHandler.INSTANCE;
                }
                return typedObject.getTypeID() == Types.BLOB
                    ? MimerBLOBValueHandler.INSTANCE
                    : MimerCLOBValueHandler.INSTANCE;
            default:
                return null;
        }
    }
}
