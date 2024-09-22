/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

/**
 * @author Chao Tian
 */
public class GBase8sTable extends GenericTable {

    public GBase8sTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType,
            @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    public GBase8sTable(GenericStructContainer container, String tableName, String tableCatalogName,
            String tableSchemaName) {
        super(container, tableName, tableCatalogName, tableSchemaName);
    }

}
