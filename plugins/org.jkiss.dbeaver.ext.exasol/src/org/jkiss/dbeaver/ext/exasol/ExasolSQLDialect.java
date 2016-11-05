/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;

import java.sql.SQLException;

public class ExasolSQLDialect extends JDBCSQLDialect {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    
    //Exasol does not support prepareCall
    public static final String[] EXEC_KEYWORDS = new String[]{};


    public ExasolSQLDialect(JDBCDatabaseMetaData metaData) {
        super("Exasol", metaData);
        try {
            for (String kw : metaData.getSQLKeywords().split(",")) {
                this.addSQLKeyword(kw);
            }
        } catch (SQLException e) {
            LOG.warn("Could not retrieve reserved keyword list from Exasol dictionary");
        }

    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[]{};
    }
   
}

