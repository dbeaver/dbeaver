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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class TiberoDataTypeCache extends JDBCBasicDataTypeCache<TiberoDataSource, JDBCDataType> {

    TiberoDataTypeCache(@NotNull TiberoDataSource owner) {
        super(owner);
    }

    @Override
    public void setCache(@NotNull List<JDBCDataType> objects) {
        Set<String> names = new LinkedHashSet<>();
        List<JDBCDataType> uniqueTypes = new ArrayList<>(objects.size());
        for (JDBCDataType type : objects) {
            String name = type.getName();
            if (name == null || names.add(name.toUpperCase(Locale.ENGLISH))) {
                uniqueTypes.add(type);
            }
        }
        super.setCache(uniqueTypes);
    }
}
