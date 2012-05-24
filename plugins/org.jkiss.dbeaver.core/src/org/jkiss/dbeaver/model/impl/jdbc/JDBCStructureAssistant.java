/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
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
    static protected final Log log = LogFactory.getLog(JDBCStructureAssistant.class);

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
    public Collection<DBSObject> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        int maxResults)
        throws DBException
    {
        List<DBSObject> objects = new ArrayList<DBSObject>();
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, CoreMessages.model_jdbc_find_objects_by_name);
        try {
            for (DBSObjectType type : objectTypes) {
                findObjectsByMask(context, type, parentObject, objectNameMask, maxResults - objects.size(), objects);
                if (objects.size() >= maxResults) {
                    break;
                }
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
        return objects;
    }

    protected abstract void findObjectsByMask(
        JDBCExecutionContext context,
        DBSObjectType objectType,
        DBSObject parentObject,
        String objectNameMask,
        int maxResults,
        List<DBSObject> objects)
        throws DBException, SQLException;

}
