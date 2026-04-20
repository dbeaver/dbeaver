/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.access;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthPromptField;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Auth model that can explicitly describe which credential properties
 * should be requested interactively during connection initialization.
 */
public interface DBAAuthModelPromptProperties<CREDENTIALS extends DBAAuthCredentials> {

    /**
     * Builds the list of prompt fields for the given credentials.
     * Returns empty list if no fields need to be prompted.
     */
    @NotNull
    List<DBPAuthPromptField> buildPromptFields(
        @Nullable DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull CREDENTIALS credentials
    );

    /**
     * Updates credentials object from the values provided by the user.
     */
    void updateCredentials(
        @NotNull CREDENTIALS credentials,
        @NotNull Map<String, String> promptValues
    );
}
