/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic data source info
 */
class GenericDataSourceInfo extends JDBCDataSourceInfo {

    private static List<String> EXEC_KEYWORDS = new ArrayList<String>();

    static {
        EXEC_KEYWORDS.add("EXEC");
        //EXEC_KEYWORDS.add("EXECUTE");
        EXEC_KEYWORDS.add("CALL");
        //EXEC_KEYWORDS.add("BEGIN");
        //EXEC_KEYWORDS.add("DECLARE");
    }

    public GenericDataSourceInfo(DBSObject owner, JDBCDatabaseMetaData metaData)
    {
        super(owner, metaData);
    }

    @Override
    public Collection<String> getExecuteKeywords()
    {
        return EXEC_KEYWORDS;
    }
}
