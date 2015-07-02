/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ModelPreferences;

/**
 * Object name case transformer
 */
public class DBObjectNameCaseTransformer implements IPropertyValueTransformer<DBSObject, String> {

    @Override
    public String transform(DBSObject object, String value)
    {
        return transformName(object.getDataSource(), value);
    }

    public static String transformObjectName(DBSObject object, String value)
    {
        return transformName(object.getDataSource(), value);
    }

    @Nullable
    public static String transformName(@NotNull DBPDataSource dataSource, @Nullable String value)
    {
        if (value == null) {
            return null;
        }
        final boolean isNameCaseSensitive = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CASE_SENSITIVE);
        if (isNameCaseSensitive || !(dataSource instanceof SQLDataSource)) {
            return value;
        }
        final SQLDialect dialect = ((SQLDataSource)dataSource).getSQLDialect();
        if (DBUtils.isQuotedIdentifier(dataSource, value)) {
            if (dialect.supportsQuotedMixedCase()) {
                return value;
            }
        }
        if (dialect.supportsUnquotedMixedCase()) {
            return value;
        }
        return dialect.storesUnquotedCase().transform(value);
    }

}
