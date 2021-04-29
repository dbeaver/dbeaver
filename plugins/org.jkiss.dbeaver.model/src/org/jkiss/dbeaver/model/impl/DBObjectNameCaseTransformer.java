/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObject;

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

        final SQLDialect dialect = dataSource.getSQLDialect();
        final boolean isNameCaseSensitive = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CASE_SENSITIVE) ||
            dialect.storesUnquotedCase() == DBPIdentifierCase.MIXED;
        if (isNameCaseSensitive) {
            return value;
        }
        if (DBUtils.isQuotedIdentifier(dataSource, value)) {
            if (dialect.supportsQuotedMixedCase()) {
                return value;
            }
            value = DBUtils.getUnQuotedIdentifier(dataSource, value);
        } else {
            if (dialect.supportsUnquotedMixedCase() || dialect.storesUnquotedCase() == null) {
                return value;
            }
        }

        String xName = dialect.storesUnquotedCase().transform(value);
        if (!DBUtils.getQuotedIdentifier(dataSource, xName).equals(xName)) {
            // Name contains special characters and has to be quoted - leave it as is
            return value;
        }
        return xName;
    }

}
