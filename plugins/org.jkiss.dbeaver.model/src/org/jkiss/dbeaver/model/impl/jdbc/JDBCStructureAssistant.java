/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
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
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
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

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(
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
