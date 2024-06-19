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

package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilegeOwner;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.Map;

/**
 * PostgrePrivilegeOwner
 */
public interface PostgrePrivilegeOwner extends PostgreObject, DBAPrivilegeOwner {

    @DPIContainer
    PostgreSchema getSchema();

    PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException;

    /**
     * Get object privileges.
     * @param includeNestedObjects - include permissions for all nested objects. For exmaple for table columns.
     */
    @Override
    Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException;

    String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options);
}
