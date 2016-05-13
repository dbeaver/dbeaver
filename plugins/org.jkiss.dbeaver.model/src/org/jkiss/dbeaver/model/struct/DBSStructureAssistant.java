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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSStructureAssistant
 */
public interface DBSStructureAssistant
{

    DBSObjectType[] getSupportedObjectTypes();

    DBSObjectType[] getHyperlinkObjectTypes();

    DBSObjectType[] getAutoCompleteObjectTypes();

    /**
     * Search objects matching specified mask.
     * @param monitor           monitor
     * @param parentObject      parent (schema or catalog)
     * @param objectTypes       type of objects to search
     * @param objectNameMask    name mask
     * @param caseSensitive     case sensitive search (ignored by some implementations)
     * @param globalSearch      search in all available schemas/catalogs. If false then search with respect of active schema/catalog
     * @param maxResults        maximum number of results
     * @return object references
     * @throws DBException
     */
    Collection<DBSObjectReference> findObjectsByMask(
        DBRProgressMonitor monitor,
        @Nullable DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch,
        int maxResults) throws DBException;

}