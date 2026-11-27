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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCharset;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCollation;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEngine;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class MySQLTableManagerTest extends DBeaverUnitTest {

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    public void tableCollationIsExplicitlyDefault(boolean compact, boolean includeCharset) throws Exception {
        var dataSource = Mockito.mock(MySQLDataSource.class);
        Mockito.when(dataSource.supportsCharsets()).thenReturn(true);
        Mockito.when(dataSource.supportsCollations()).thenReturn(true);
        var resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString(MySQLConstants.COL_CHARSET)).thenReturn("utf8mb4");
        Mockito.when(resultSet.getString(MySQLConstants.COL_COLLATION)).thenReturn("utf8mb4_general_ci");
        var charset = new MySQLCharset(dataSource, resultSet);
        var collation = new MySQLCollation(charset, resultSet);
        var table = new MySQLTable(new MySQLCatalog(dataSource, null));
        var monitor = new VoidProgressMonitor();
        var info = table.getAdditionalInfo(monitor);
        info.setEngine(new MySQLEngine(dataSource, "InnoDB"));
        if (includeCharset) {
            info.setCharset(charset);
        }
        info.setCollation(collation);
        Map<String, Object> options = new HashMap<>();
        options.put(DBPScriptObject.OPTION_SCRIPT_FORMAT_COMPACT, compact);
        var manager = new MySQLTableManager();
        var ddl = new StringBuilder();

        manager.appendTableModifiers(monitor, table, manager.makeCreateCommand(table, options), ddl, false, options);

        String delimiter = MySQLTableManager.getDelimiter(options);
        Assertions.assertEquals(
            delimiter + "ENGINE=InnoDB" +
                (includeCharset ? delimiter + "DEFAULT CHARSET=utf8mb4" : "") +
                delimiter + "DEFAULT COLLATE=utf8mb4_general_ci",
            ddl.toString()
        );
    }
}
