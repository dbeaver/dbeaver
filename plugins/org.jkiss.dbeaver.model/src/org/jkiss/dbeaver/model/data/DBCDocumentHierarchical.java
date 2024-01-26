/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

public interface DBCDocumentHierarchical<ENTITY extends DBSEntity> {
    /**
     * Some databases can have tables/collections
     * associated with specific data entry
     * in that situation we avoid reading them as typical metadata entity
     * So we load them as DBSEntities on demand and never show them as
     * navigator nodes
     *
     * @return list of DBSEntity elements associated with the current row
     */
    List<ENTITY> listChildrenEntities(@NotNull DBRProgressMonitor monitor) throws DBCException;

    void createChildEntity(String name, @NotNull DBRProgressMonitor monitor) throws DBCException;

    void deleteChildEntity(String name, @NotNull DBRProgressMonitor monitor) throws DBCException;
}
