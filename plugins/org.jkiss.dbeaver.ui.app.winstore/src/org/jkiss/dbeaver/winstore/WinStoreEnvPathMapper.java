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
package org.jkiss.dbeaver.winstore;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.IEnvironmentPathMapper;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.nio.file.Path;

public class WinStoreEnvPathMapper implements IEnvironmentPathMapper {
    private static final Log log = Log.getLog(WinStoreEnvPathMapper.class);

    private static final String WINDOWS_APP_LOCAL_DATA_PACKAGE = "DBeaverCorp.DBeaverCE_1b7tdvn0p0f9y";
    private static final String APP_DATA_ROAMING_PATH_STRING = System.getenv("AppData");
    private static final String LOCAL_APP_DATA_PATH_STRING = System.getenv("LOCALAPPDATA");
    private static final String USER_HOME_PATH_STRING = System.getProperty("user.home");
    
    private final Path realVirtualizedRoot;
    
    public WinStoreEnvPathMapper() {
        Path localAppDataPath = LOCAL_APP_DATA_PATH_STRING != null
            ? Path.of(LOCAL_APP_DATA_PATH_STRING)
            : Path.of(USER_HOME_PATH_STRING, "AppData", "Local");
        
        realVirtualizedRoot = localAppDataPath.resolve("Packages")
            .resolve(WINDOWS_APP_LOCAL_DATA_PACKAGE).resolve("LocalCache").resolve("Roaming");
    }

    @Override
    public boolean isApplicable(@NotNull String localEnvPath) {
        return RuntimeUtils.isWindowsStoreApplication() && localEnvPath.startsWith(APP_DATA_ROAMING_PATH_STRING);
    }

    @NotNull
    @Override
    public String map(@NotNull String localEnvPath) {
        Path remappedPath = realVirtualizedRoot.resolve(Path.of(APP_DATA_ROAMING_PATH_STRING).relativize(Path.of(localEnvPath)));
        String resultPath = remappedPath.toString();
        
        log.debug("Remapping file path [" + localEnvPath + "] to [" + resultPath + "]");
        return resultPath;
    }

}
