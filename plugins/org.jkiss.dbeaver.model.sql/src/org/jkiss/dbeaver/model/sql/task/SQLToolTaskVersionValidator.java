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
package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.struct.DBSObject;


public abstract class SQLToolTaskVersionValidator<SETTINGS extends SQLToolExecuteSettings<? extends DBSObject>, Object>
        implements IPropertyValueValidator<SETTINGS, Object> {

    @Override
    public boolean isValidValue(SETTINGS settings, Object value) throws IllegalArgumentException {
        if (!settings.getObjectList().isEmpty()) {
            DBPDataSource dataSource = settings.getObjectList().get(0).getDataSource();
            if (dataSource instanceof JDBCDataSource) {
                return ((JDBCDataSource) dataSource).isServerVersionAtLeast(getMajorVersion(), getMinorVersion());
            }
        }
        return false;
    }

    public abstract int getMajorVersion();

    public abstract int getMinorVersion();
}
