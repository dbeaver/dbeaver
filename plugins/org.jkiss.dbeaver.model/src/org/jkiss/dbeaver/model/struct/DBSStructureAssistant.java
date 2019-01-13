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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

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
    @NotNull
    List<DBSObjectReference> findObjectsByMask(
        DBRProgressMonitor monitor,
        @Nullable DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch,
        int maxResults) throws DBException;

}