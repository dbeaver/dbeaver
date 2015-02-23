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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentAbstract implements DBDContent, DBDValueCloneable {

    protected final DBPDataSource dataSource;

    protected JDBCContentAbstract(DBPDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public abstract void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

    @Override
    public void resetContents()
    {
        // do nothing
    }

    @Override
    public String toString()
    {
        String displayString = getDisplayString(DBDDisplayFormat.UI);
        return displayString == null ? DBConstants.NULL_VALUE_LABEL : displayString;
    }
}