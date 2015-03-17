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
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * CassandraStructureAssistant
 */
public class CassandraStructureAssistant extends JDBCStructureAssistant
{
    private final CassandraDataSource dataSource;

    public CassandraStructureAssistant(CassandraDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected CassandraDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE
            };
    }

    @Override
    protected void findObjectsByMask(JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        CassandraKeyspace parentKeyspace = parentObject instanceof CassandraKeyspace ? (CassandraKeyspace)parentObject : null;
        final CassandraDataSource dataSource = getDataSource();

        DBRProgressMonitor monitor = session.getProgressMonitor();
        JDBCResultSet dbResult = session.getMetaData().getTables(
            null,
            parentKeyspace == null ? null : parentKeyspace.getName(),
            objectNameMask,
            null);
        try {
            while (dbResult.next()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_SCHEM);
                String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
                if (CommonUtils.isEmpty(tableName)) {
                    continue;
                }
                CassandraKeyspace keyspace = parentKeyspace != null ?
                    parentKeyspace :
                    CommonUtils.isEmpty(schemaName) ? null : dataSource.getSchema(schemaName);

                references.add(new KeyspaceReference(
                    keyspace,
                    tableName,
                    JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS)));
                if (references.size() >= maxResults) {
                    break;
                }
            }
        }
        finally {
            dbResult.close();
        }
    }

    private class KeyspaceReference extends AbstractObjectReference {

        private KeyspaceReference(CassandraKeyspace container, String tableName, String description)
        {
            super(tableName, container, description, RelationalObjectType.TYPE_TABLE);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {
            CassandraColumnFamily table = ((CassandraKeyspace)getContainer()).getChild(monitor, getName());
            if (table == null) {
                throw new DBException("Can't find column family '" + getName() + "' in '" + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
            }
            return table;
        }
    }

}
