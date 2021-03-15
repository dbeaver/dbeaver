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
package org.jkiss.dbeaver.ext.clickhouse.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTable;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Clickhouse table manager
 */
public class ClickhouseTableManager extends GenericTableManager {

    private static final Log log = Log.getLog(ClickhouseTableManager.class);

    @Override
    protected String getDropTableType(GenericTableBase table) {
        // Both tables and views must be deleted with DROP TABLE
        return "TABLE";
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, GenericTableBase table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        if (table instanceof ClickhouseTable) {
            try {
                List<? extends GenericTableColumn> attributes = table.getAttributes(monitor);
                if (!CommonUtils.isEmpty(attributes)) {
                    ddl.append(" ENGINE = MergeTree()\n" +
                            "ORDER BY ").append(DBUtils.getQuotedIdentifier(attributes.get(0)));
                } else {
                    ddl.append(" ENGINE = Log");
                }
            } catch (DBException e) {
                log.debug("Can't read " + table.getName() + " columns");
            }
        }
    }
}
