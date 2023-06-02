/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import java.util.Arrays;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;

public class AltibaseSQLDialect extends GenericSQLDialect {

    private static final String[] ALTIBASE_BLOCK_HEADERS = new String[]{
        "EXECUTE BLOCK",
        "DECLARE",
        "IS",
    };

    private static final String[][] ALTIBASE_BEGIN_END_BLOCK = new String[][]{
        {"BEGIN", "END"},
    };

    private static final String[] DDL_KEYWORDS = new String[] {
        "CREATE", "ALTER", "DROP", "EXECUTE", "CACHE"
    };

    private static final String[] ALTIBASE_KEYWORDS = new String[] {
        "CURRENT_USER",
        "CURRENT_ROLE",
        "NCHAR",
        "VALUE"
    };

    public AltibaseSQLDialect() {
        super("Altibase", "altibase");
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return DDL_KEYWORDS;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return ALTIBASE_BLOCK_HEADERS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return ALTIBASE_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        turnFunctionIntoKeyword("TRUNCATE");
        addKeywords(Arrays.asList(ALTIBASE_KEYWORDS), DBPKeywordType.KEYWORD);
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return super.validIdentifierPart(c, quoted) || c == '$';
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return false;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return true;
    }
}
