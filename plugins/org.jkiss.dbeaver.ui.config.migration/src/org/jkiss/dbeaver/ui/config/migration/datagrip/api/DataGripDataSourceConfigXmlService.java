/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.migration.datagrip.api;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Interface for importing Idea data source configs. Mainly aimed to work with .idea folder in a project
 * and home directory of JetBrains.
 */
public interface DataGripDataSourceConfigXmlService {

    /**
     * Build a map where key is uuid of the datasource against config
     *
     * @param pathToIdeaFolder path to project folder which consist .idea folder.
     */
    @NotNull Map<String, Map<String, String>> buildIdeaConfigProps(@NotNull String pathToIdeaFolder) throws Exception;

    /**
     * Build a ImportConnectionInfo by single entry of {@link DataGripDataSourceConfigXmlService#buildIdeaConfigProps(String)}
     */
    @NotNull ImportConnectionInfo buildIdeaConnectionFromProps(@NotNull Map<String, String> conProps);

    /**
     * Trying to extract a path to most recent project folder by iterating idea folder in JetBrains home folder
     *
     * @return path to most recent project
     */
    @NotNull
    List<Path> tryExtractRecentProjectPath();
}
