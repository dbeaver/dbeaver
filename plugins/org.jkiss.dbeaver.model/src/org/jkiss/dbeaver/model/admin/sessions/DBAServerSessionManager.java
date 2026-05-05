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

package org.jkiss.dbeaver.model.admin.sessions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.Collection;
import java.util.Map;

/**
 * Session manager
 */
public interface DBAServerSessionManager<SESSION_TYPE extends DBAServerSession> {

    @NotNull
    DBPDataSource getDataSource();

    @NotNull
    Collection<SESSION_TYPE> getSessions(@NotNull DBCSession session, @NotNull Map<String, Object> options)
        throws DBException;

    void alterSession(@NotNull DBCSession session, @NotNull String sessionId, @NotNull Map<String, Object> options)
        throws DBException;

    /**
     * Returns the options for terminating sessions.
     * @return map containing session termination options, with option keys and their corresponding values
     */
    @NotNull
    Map<String, Object> getTerminateOptions();

}
