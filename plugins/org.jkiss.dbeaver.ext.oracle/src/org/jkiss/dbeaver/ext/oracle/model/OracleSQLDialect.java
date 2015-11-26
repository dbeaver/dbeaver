/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;

import java.util.Collection;
import java.util.Collections;

/**
* Oracle SQL dialect
*/
class OracleSQLDialect extends JDBCSQLDialect {
    private static final String[] ORACLE_LINE_COMMENTS = {
            "--",
            //"^rem"
    };

    public OracleSQLDialect(OracleDataSource oracleDataSource, JDBCDatabaseMetaData metaData) {
        super(oracleDataSource, "Oracle", metaData);
        addSQLKeyword("ANALYZE");
        addSQLKeyword("VALIDATE");
        addSQLKeyword("STRUCTURE");
        addSQLKeyword("COMPUTE");
        addSQLKeyword("STATISTICS");
    }

    @NotNull
    @Override
    public Collection<String> getExecuteKeywords() {
        return Collections.singleton("call");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String[] getSingleLineComments() {
        return ORACLE_LINE_COMMENTS;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return true;
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return true;
    }
}
