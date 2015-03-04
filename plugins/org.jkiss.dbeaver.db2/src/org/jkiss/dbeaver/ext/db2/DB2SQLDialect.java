/*
 * Copyright (C) 2013-2014 Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;

/**
 * DB2 SQL dialect
 * 
 * @author Denis Forveille
 * 
 */
public class DB2SQLDialect extends JDBCSQLDialect {

    public DB2SQLDialect(DB2DataSource db2DataSource, JDBCDatabaseMetaData metaData)
    {
        super(db2DataSource, "DB2", metaData);
        for (String kw : DB2Constants.ADVANCED_KEYWORDS) {
            this.addSQLKeyword(kw);
        }
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode()
    {
        return MultiValueInsertMode.GROUP_ROWS;
    }

}
