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

package org.jkiss.dbeaver.ext.tidb.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.tidb.mysql.model.TiDBMySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

public class TiDBPlanAnalyzer extends MySQLPlanAnalyser {
    private static final String[] FIRST_KEYWORD_BLOCK_LIST = new String[]{
        "DESC", "SET", "EXPLAIN"
    };
    private TiDBMySQLDataSource dataSource;

    public TiDBPlanAnalyzer(TiDBMySQLDataSource dataSource) {
        super(dataSource);

        this.dataSource = dataSource;
    }

    private static boolean block(String firstKeyword) {
        for (String blockWord : FIRST_KEYWORD_BLOCK_LIST) {
            if (!blockWord.equalsIgnoreCase(firstKeyword)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public MySQLPlanAbstract explain(JDBCSession session, String query) throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(this.dataSource);
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        if (TiDBPlanAnalyzer.block(firstKeyword)) {
            throw new DBCException("This statement could not produce execution plan");
        }

        return new TiDBPlainClassic(session, query);
    }
}
