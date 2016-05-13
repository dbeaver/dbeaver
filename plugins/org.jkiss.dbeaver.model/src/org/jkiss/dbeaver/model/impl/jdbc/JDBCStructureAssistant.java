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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBCStructureAssistant
 */
public abstract class JDBCStructureAssistant implements DBSStructureAssistant 
{
    static protected final Log log = Log.getLog(JDBCStructureAssistant.class);

    protected abstract JDBCDataSource getDataSource();

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] { RelationalObjectType.TYPE_TABLE };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] { RelationalObjectType.TYPE_TABLE };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] { RelationalObjectType.TYPE_TABLE };
    }

    @Override
    public Collection<DBSObjectReference> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch,
        int maxResults)
        throws DBException
    {
        List<DBSObjectReference> references = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), ModelMessages.model_jdbc_find_objects_by_name)) {
            for (DBSObjectType type : objectTypes) {
                findObjectsByMask(session, type, parentObject, objectNameMask, caseSensitive, globalSearch, maxResults - references.size(), references);
                if (references.size() >= maxResults) {
                    break;
                }
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
        return references;
    }

    protected abstract void findObjectsByMask(
        JDBCSession session,
        DBSObjectType objectType,
        DBSObject parentObject,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch, int maxResults,
        List<DBSObjectReference> references)
        throws DBException, SQLException;

}
