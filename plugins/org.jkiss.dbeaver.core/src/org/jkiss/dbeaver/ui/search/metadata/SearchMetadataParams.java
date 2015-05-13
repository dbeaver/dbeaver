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
package org.jkiss.dbeaver.ui.search.metadata;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.List;

/**
 * Search parameters
 */
public class SearchMetadataParams {

    private DBSObject parentObject;
    private List<DBSObjectType> objectTypes;
    private String objectNameMask;
    private boolean caseSensitive;
    private int maxResults;
    private int matchType;

    public SearchMetadataParams()
    {
    }

    public DBSObject getParentObject()
    {
        return parentObject;
    }

    public void setParentObject(DBSObject parentObject)
    {
        this.parentObject = parentObject;
    }

    public List<DBSObjectType> getObjectTypes()
    {
        return objectTypes;
    }

    public void setObjectTypes(List<DBSObjectType> objectTypes)
    {
        this.objectTypes = objectTypes;
    }

    public String getObjectNameMask()
    {
        return objectNameMask;
    }

    public void setObjectNameMask(String objectNameMask)
    {
        this.objectNameMask = objectNameMask;
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public int getMatchType()
    {
        return matchType;
    }

    public void setMatchType(int matchType)
    {
        this.matchType = matchType;
    }
}
