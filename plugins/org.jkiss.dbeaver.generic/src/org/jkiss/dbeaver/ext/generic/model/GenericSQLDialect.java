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
class GenericSQLDialect extends JDBCSQLDialect {

    private static List<String> EXEC_KEYWORDS = new ArrayList<String>();

    static {
        EXEC_KEYWORDS.add("EXEC");
        //EXEC_KEYWORDS.add("EXECUTE");
        EXEC_KEYWORDS.add("CALL");
        //EXEC_KEYWORDS.add("BEGIN");
        //EXEC_KEYWORDS.add("DECLARE");
    }

    private final String scriptDelimiter;

    public GenericSQLDialect(GenericDataSource dataSource, JDBCDatabaseMetaData metaData)
    {
        super(dataSource, "Generic", metaData);
        scriptDelimiter = CommonUtils.toString(dataSource.getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SCRIPT_DELIMITER));
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

}
