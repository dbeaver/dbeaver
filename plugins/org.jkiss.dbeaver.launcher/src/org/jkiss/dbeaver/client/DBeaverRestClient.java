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
package org.jkiss.dbeaver.client;

import org.jkiss.dbeaver.launcher.DBeaverLauncher;

import java.nio.file.Path;

public class DBeaverRestClient {

    public static Integer getDBeaverServerPort(Path dbeaverDataPath) {
        return 1;
    }

    public static boolean supportsAutoLaunch() {
        String launchPath = System.getenv(DBeaverLauncher.PROP_LAUNCHER);
        return launchPath != null && !launchPath.trim().isEmpty();
    }
}
