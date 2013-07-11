/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

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

    public static String transformName(DBPDataSource dataSource, String value)
    {
        final boolean isNameCaseSensitive = dataSource.getContainer().getPreferenceStore().getBoolean(PrefConstants.META_CASE_SENSITIVE);
        if (isNameCaseSensitive) {
            return value;
        }
        final DBPDataSourceInfo info = dataSource.getInfo();
        if (!info.supportsQuotedMixedCase() && !info.supportsUnquotedMixedCase()) {
            // Mixed case not supported - so leave it as is
            return value;
        }
        return info.storesUnquotedCase().transform(value);
    }

}
