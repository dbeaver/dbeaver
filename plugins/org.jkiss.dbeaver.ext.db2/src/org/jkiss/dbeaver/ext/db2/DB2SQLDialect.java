/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;

/**
 * DB2 SQL dialect
 * 
 * @author Denis Forveille
 * 
 */
public class DB2SQLDialect extends JDBCSQLDialect {

    public static final String[] EXEC_KEYWORDS = new String[]{"call"};

    private static final String[][] DB2_BEGIN_END_BLOCK = new String[][]{
    };

    public DB2SQLDialect() {
        super("DB2 LUW");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        for (String kw : DB2Constants.ADVANCED_KEYWORDS) {
            this.addSQLKeyword(kw);
        }
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode()
    {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords()
    {
        return EXEC_KEYWORDS;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return "SYSIBM.SYSDUMMY1";
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexString.INSTANCE;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return DB2_BEGIN_END_BLOCK;
    }
}
