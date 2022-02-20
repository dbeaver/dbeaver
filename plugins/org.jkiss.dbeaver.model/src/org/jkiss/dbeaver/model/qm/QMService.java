/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;


public interface QMService extends QMEventBrowser {
    void saveEvent(QMMetaEvent event, DBRProgressMonitor monitor) throws DBException, SQLException;

    @NotNull
    Collection<String> getQueryFilterHistory(@NotNull String query) throws DBException;

    void saveQueryFilterValue(@NotNull String query, @NotNull String filterValue) throws DBException;

    void deleteQueryFilterValue(@NotNull String query, String filterValue) throws DBException;

    String openSession(@NotNull QMSessionInfo sessionInfo, @NotNull DBRProgressMonitor monitor) throws DBException;

    void closeSession(String appSessionId, DBRProgressMonitor monitor);
}
