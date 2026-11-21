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
package org.jkiss.dbeaver.model.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

/** The local projects visible to the current DataDam account. */
public class DDWorkspace {
    private static final Log log = Log.getLog(DDWorkspace.class);

    @Nullable
    private static DDWorkspace instance;

    private final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
    private final DDProjectManager projectManager = DDProjectManager.getInstance();

    private DDWorkspace() {
    }

    @NotNull
    public static synchronized DDWorkspace getInstance() {
        if (instance == null) {
            instance = new DDWorkspace();
        }
        return instance;
    }

    @NotNull
    public List<? extends DBPProject> getProjects() {
        return workspace.getProjects().stream().filter(project -> {
            try {
                return isVisibleInCurrentAccount(project);
            } catch (DBException | IllegalStateException e) {
                log.debug("Cannot read DataDam project binding", e);
                return false;
            }
        }).toList();
    }

    public boolean isVisibleInCurrentAccount(@NotNull DBPProject project) throws DBException {
        DDProjectManager.BindingStatus status = projectManager.getBindingStatus(project);
        return status == DDProjectManager.BindingStatus.UNBOUND
            || status == DDProjectManager.BindingStatus.CURRENT_ACCOUNT;
    }
}
