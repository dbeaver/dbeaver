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

package org.jkiss.dbeaver.ext.clickhouse;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;

public class ClickhouseDataSourceInfo extends JDBCDataSourceInfo {

    public ClickhouseDataSourceInfo(JDBCDatabaseMetaData metaData) {
        super(metaData);
    }

    @Override
    public boolean supportsIndexes() {
        // For now - Clickhouse driver return us empty list as indexInfo and we can't create Clickhouse indexes via DBeaver UI
        // So far we turn off indexes
        return false;
    }

}