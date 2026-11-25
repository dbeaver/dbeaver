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
package org.jkiss.dbeaver.tools.transfer.ui.handlers;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DataImportPropertyTester extends PropertyTester {
    @Override
    public boolean test(
        @Nullable Object receiver,
        @NotNull String property,
        @NotNull Object[] args,
        @Nullable Object expectedValue
    ) {
        if (!(receiver instanceof DBSObject object)) {
            return false;
        }
        DBSObject publicObject = DBUtils.getPublicObject(object);
        DBPDataSource dataSource = publicObject == null ? null : publicObject.getDataSource();
        return supportsImport(dataSource);
    }

    public static boolean supportsImport(@Nullable DBPDataSource dataSource) {
        return dataSource == null || !Boolean.FALSE.equals(dataSource.getDataSourceFeature(DBPDataSource.FEATURE_DATA_IMPORT));
    }
}
