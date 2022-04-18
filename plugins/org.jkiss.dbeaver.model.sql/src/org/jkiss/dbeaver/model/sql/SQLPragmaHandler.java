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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

import java.util.Map;

public interface SQLPragmaHandler {
    String PRAGMA_EXPORT = "export";

    /**
     * Whether the pragma was processed and should be removed from the {@code context}
     */
    int RESULT_POP_PRAGMA = 1;

    /**
     * Whether the processed query should not be run
     */
    int RESULT_SKIP_QUERY = 1 << 1;

    /**
     * @return a set of {@code RESULT_} constants.
     * @throws DBException on any error
     */
    int processPragma(@NotNull DBRProgressMonitor monitor, @NotNull DBSDataContainer container, @NotNull Map<String, Object> params) throws DBException;
}
