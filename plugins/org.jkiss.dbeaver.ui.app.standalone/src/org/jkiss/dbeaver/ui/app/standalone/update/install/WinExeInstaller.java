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
package org.jkiss.dbeaver.ui.app.standalone.update.install;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.nio.file.Path;

public final class WinExeInstaller implements PlatformInstaller {
    @Override
    public void run(@NotNull Path executable, @NotNull Log log) throws Exception {
        final String path = executable.toString();
        Runtime.getRuntime().exec(new String[]{
            "cmd.exe", "/C",
            "start", "/W", path, "&&", "del", path,
        });
    }
}
