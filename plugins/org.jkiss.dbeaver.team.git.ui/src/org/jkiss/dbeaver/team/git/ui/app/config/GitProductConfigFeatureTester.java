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
package org.jkiss.dbeaver.team.git.ui.app.config;

import org.eclipse.jgit.lib.Constants;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureTester;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;

public final class GitProductConfigFeatureTester implements ProductConfigFeatureTester {
    private static final Log log = Log.getLog(GitProductConfigFeatureTester.class);

    @NotNull
    @Override
    public Enablement isFeatureEnabled() {
        for (var project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            try {
                if (Files.isDirectory(project.getAbsolutePath().resolve(Constants.DOT_GIT))) {
                    return Enablement.EXPLICITLY_ENABLED;
                }
            } catch (IllegalStateException e) {
                log.debug("Can't determine Git feature enablement for project " + project.getName(), e);
            }
        }
        return Enablement.UNDEFINED;
    }
}
