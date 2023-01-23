/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Session context.
 * Holds various auth sessions.
 */
public interface SMSessionContext {

    /**
     * Find and opens space session
     * @param space target space
     * @param open  if true then new session will be opened if possible
     */
    @Nullable
    SMSession getSpaceSession(@NotNull DBRProgressMonitor monitor, @NotNull SMAuthSpace space, boolean open) throws DBException;

    @Nullable
    /**
     * Workaround to determine the correct context in QM
     */
    SMAuthSpace getPrimaryAuthSpace();

    @Nullable
    SMSession findSpaceSession(@NotNull SMAuthSpace space);

    void addSession(@NotNull SMSession session);

    boolean removeSession(@NotNull SMSession session);

}
