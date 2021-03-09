/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public interface DBPReferentialIntegrityController {
    boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) throws DBException;

    void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException;

    /**
     * Returns description of things that may go wrong when changing referential integrity setting back and forth.
     * When changing referential integrity is not supported, returns an empty string rather than null
     * to avoid callers making redundant null checks
     * (callers should check if controller supports changing referential integrity anyway).
     *
     * @param monitor monitor
     * @return caveats description, never {@code null}
     * @throws DBException if unable to retrieve caveats for any reason
     */
    @NotNull
    String getReferentialIntegrityDisableWarning(@NotNull DBRProgressMonitor monitor) throws DBException;
}
