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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

/**
 * Settings contributed to the AI connection preferences page.
 * Sections save to the connection supplied by the page, which may be a wizard validation copy.
 */
public interface AIConnectionSettingsSection {
    boolean appliesTo(@NotNull DBPDataSourceContainer connection);

    void createControls(
        @NotNull Composite parent,
        @NotNull DBPDataSourceContainer connection,
        @Nullable DBRRunnableContext runnableContext
    );

    void save(@NotNull DBPDataSourceContainer connection);
}
