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

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic data source info
 */
public class GenericSQLDialect extends JDBCSQLDialect {

    private static List<String> EXEC_KEYWORDS = new ArrayList<String>();

    static {
        EXEC_KEYWORDS.add("EXEC");
        //EXEC_KEYWORDS.add("EXECUTE");
        EXEC_KEYWORDS.add("CALL");
        //EXEC_KEYWORDS.add("BEGIN");
        //EXEC_KEYWORDS.add("DECLARE");
    }

    private final String scriptDelimiter;
    private final boolean legacySQLDialect;

    public GenericSQLDialect(GenericDataSource dataSource, JDBCDatabaseMetaData metaData)
    {
        super(dataSource, "Generic", metaData);
        scriptDelimiter = CommonUtils.toString(dataSource.getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER));
        legacySQLDialect = CommonUtils.toBoolean(dataSource.getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_LEGACY_DIALECT));
    }

    @NotNull
    @Override
    public String getScriptDelimiter()
    {
        return CommonUtils.isEmpty(scriptDelimiter) ? super.getScriptDelimiter() : scriptDelimiter;
    }

    @NotNull
    @Override
    public Collection<String> getExecuteKeywords()
    {
        return EXEC_KEYWORDS;
    }

    public boolean isLegacySQLDialect() {
        return legacySQLDialect;
    }
}
