/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;

/**
 * Filter which works differently depending on node type
 */
public class SmartNavigatorTreeFilter extends SimpleNavigatorTreeFilter {

    private final DatabaseNavigatorTreeFilter dbFilter;

    public SmartNavigatorTreeFilter() {
        dbFilter = new DatabaseNavigatorTreeFilter();
    }

    public SmartNavigatorTreeFilter(DatabaseNavigatorTreeFilter dbFilter) {
        this.dbFilter = dbFilter;
    }

    @Override
    public boolean filterFolders() {
        return false;
    }

    @Override
    public boolean isLeafObject(Object object) {
        if (object instanceof DBNDatabaseNode) {
            return dbFilter.isLeafObject(object);
        }
        return super.isLeafObject(object);
    }

    @Override
    public boolean filterObjectByPattern(Object object) {
        if (object instanceof DBNDatabaseNode) {
            return dbFilter.filterObjectByPattern(object);
        }
        return super.filterObjectByPattern(object);
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest instanceof DBNDatabaseNode) {
            return dbFilter.select(toTest);
        }
        return super.select(toTest);
    }
}
