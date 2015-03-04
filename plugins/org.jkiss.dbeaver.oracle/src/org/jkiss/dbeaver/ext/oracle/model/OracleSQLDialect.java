/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
}
