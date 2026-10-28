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
package org.jkiss.dbeaver.ext.mysql.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionValidator;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;

public class MySQLConnectionValidatorFactory implements IAdapterFactory {
    private static final JDBCConnectionValidator VALIDATOR = new MySQLConnectionSecurity();

    @Nullable
    @Override
    public <T> T getAdapter(@NotNull Object adaptableObject, @NotNull Class<T> adapterType) {
        if (adaptableObject instanceof JDBCDataSource && adapterType == JDBCConnectionValidator.class) {
            return adapterType.cast(VALIDATOR);
        }
        return null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[] {JDBCConnectionValidator.class};
    }
}
