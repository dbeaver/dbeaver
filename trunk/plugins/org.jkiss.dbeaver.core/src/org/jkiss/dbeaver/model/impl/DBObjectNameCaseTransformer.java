/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.dbeaver.DBeaverPreferences;

/**
 * Object name case transformer
 */
public class DBObjectNameCaseTransformer implements IPropertyValueTransformer<DBSObject, String> {

    @Override
    public String transform(DBSObject object, String value)
    {
        return transformName(object.getDataSource(), value);
    }

    public static String transformName(DBSObject object, String value)
    {
        return transformName(object.getDataSource(), value);
    }

    @Nullable
    public static String transformName(@NotNull DBPDataSource dataSource, @Nullable String value)
    {
        if (value == null) {
            return null;
        }
        final boolean isNameCaseSensitive = dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.META_CASE_SENSITIVE);
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
