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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class BaseLocalPlatform extends BasePlatformImpl {
    private static final Log log = Log.getLog(BaseLocalPlatform.class);
    private static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$

    protected Path tempFolder;

    @NotNull
    public Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        if (tempFolder == null) {
            // Make temp folder
            try {
                String tempFolderPath = System.getProperty("dbeaver.io.tmpdir");
                if (!CommonUtils.isEmpty(tempFolderPath)) {
                    tempFolderPath = GeneralUtils.replaceVariables(tempFolderPath, new SystemVariablesResolver());

                    File dbTempFolder = new File(tempFolderPath);
                    if (!dbTempFolder.mkdirs()) {
                        throw new IOException("Can't create temp directory '" + dbTempFolder.getAbsolutePath() + "'");
                    }
                } else {
                    tempFolderPath = System.getProperty(StandardConstants.ENV_TMP_DIR);
                }
                monitor.subTask("Create temp folder '" + tempFolderPath + "'");
                Path tmpFolder = Paths.get(tempFolderPath);
                if (!Files.exists(tmpFolder)) {
                    log.debug("Create global temp folder '" + tmpFolder + "'");
                    Files.createDirectories(tmpFolder);
                }
                tempFolder = Files.createTempDirectory(tmpFolder, TEMP_PROJECT_NAME);
            } catch (IOException e) {
                final String sysTempFolder = System.getProperty(StandardConstants.ENV_TMP_DIR);
                if (!CommonUtils.isEmpty(sysTempFolder)) {
                    tempFolder = Path.of(sysTempFolder).resolve(TEMP_PROJECT_NAME);
                    if (!Files.exists(tempFolder)) {
                        try {
                            Files.createDirectories(tempFolder);
                        } catch (IOException ex) {
                            final String sysUserFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
                            if (!CommonUtils.isEmpty(sysUserFolder)) {
                                tempFolder = Path.of(sysUserFolder).resolve(TEMP_PROJECT_NAME);
                                if (!Files.exists(tempFolder)) {
                                    try {
                                        Files.createDirectories(tempFolder);
                                    } catch (IOException exc) {
                                        tempFolder = Path.of(TEMP_PROJECT_NAME);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Path localTemp = name == null ? tempFolder : tempFolder.resolve(name);
        if (!Files.exists(localTemp)) {
            try {
                Files.createDirectories(localTemp);
            } catch (IOException e) {
                log.error("Can't create temp directory " + localTemp, e);
            }
        }
        return localTemp;
    }
}
