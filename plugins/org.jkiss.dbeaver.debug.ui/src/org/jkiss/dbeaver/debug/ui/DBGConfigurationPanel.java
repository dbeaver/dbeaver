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
package org.jkiss.dbeaver.debug.ui;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.Map;

public interface DBGConfigurationPanel {
    
    void createPanel(@NotNull Composite parent, DBGConfigurationPanelContainer container);

    void loadConfiguration(@NotNull DBPDataSourceContainer dataSource, @NotNull Map<String, Object> configuration);

    void saveConfiguration(@NotNull DBPDataSourceContainer dataSource, @NotNull Map<String, Object> configuration);

    boolean isValid();
}
