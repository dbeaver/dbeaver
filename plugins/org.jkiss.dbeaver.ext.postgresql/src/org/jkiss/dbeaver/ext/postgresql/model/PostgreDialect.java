/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreBinaryFormatter;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;

import java.util.Arrays;
import java.util.Collections;

/**
* PostgreSQL dialect
*/
class PostgreDialect extends JDBCSQLDialect {

    public PostgreDialect(JDBCDatabaseMetaData metaData) {
        super("PostgreSQL", metaData);
        addSQLKeyword("SHOW");
        addSQLKeyword("TYPE");
        addSQLKeyword("USER");
        addSQLKeyword("COMMENT");
        addSQLKeyword("MATERIALIZED");

        addFunctions(Collections.singleton("CURRENT_DATABASE"));

        removeSQLKeyword("PUBLIC");
        removeSQLKeyword("LENGTH");
        removeSQLKeyword("LANGUAGE");
    }

/*
    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }
*/

    @Nullable
    @Override
    public String getBlockToggleString() {
        return "$$";
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return PostgreBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(PostgreConstants.DATA_TYPE_ALIASES.keySet());
    }
}
