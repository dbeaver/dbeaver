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
package org.jkiss.dbeaver.winstore;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

public class WinStoreService implements IPluginService {

    private static final Log log = Log.getLog(WinStoreService.class);

    @Override
    public void activateService() {
        // Modify JNI binaries path
        String installPath = SystemVariablesResolver.getInstallPath();
        System.setProperty("jna.boot.library.path", installPath);
        System.setProperty(DBConstants.IS_WINDOWS_STORE_APP, "true");
        log.debug("JNA boot path set to " + installPath);
    }

    @Override
    public void deactivateService() {

    }
}
