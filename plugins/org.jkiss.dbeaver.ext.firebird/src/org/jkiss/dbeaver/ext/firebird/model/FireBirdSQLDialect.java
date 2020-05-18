/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;

public class FireBirdSQLDialect extends GenericSQLDialect {

    public static final String[] FB_BLOCK_HEADERS = new String[]{
        "EXECUTE BLOCK",
        "DECLARE",
        //"IS",
    };

    public static final String[][] FB_BEGIN_END_BLOCK = new String[][]{
        {"BEGIN", "END"},
    };

    private static final String[] DDL_KEYWORDS = new String[] {
        "CREATE", "ALTER", "DROP", "EXECUTE"
    };

    public FireBirdSQLDialect() {
        super("Firebird");
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return DDL_KEYWORDS;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return FB_BLOCK_HEADERS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return FB_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
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
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        return "select * from " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
    }
}
