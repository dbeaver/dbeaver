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
package org.jkiss.dbeaver.ui.datadam.project.event;

import com.dbeaver.datadam.dam.remote.api.model.DDSharedProjectEvent;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.rpc.DDSharedProjectEventSource;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.event.actions.AbstractProjectEventJob;
import org.jkiss.dbeaver.ui.datadam.project.event.actions.ProjectConfigurationUpdatedAction;
import org.jkiss.dbeaver.ui.datadam.project.event.actions.ProjectCreatedAction;
import org.jkiss.dbeaver.ui.datadam.project.event.actions.ProjectDeletedAction;

public class DDProjectEventListener implements DDSharedProjectEventSource.Subscriber {
    private final DDProjectUIManager uiManager;

    public DDProjectEventListener(@NotNull DDProjectUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Override
    public void projectCreated(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectCreatedAction(uiManager, event));
    }

    @Override
    public void projectMetadataUpdated(@NotNull DDSharedProjectEvent event) throws DBException {
    }

    @Override
    public void projectConfigurationUpdated(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectConfigurationUpdatedAction(uiManager, event));
    }

    @Override
    public void projectDeleted(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectDeletedAction(uiManager, event));
    }

    private void schedule(@NotNull AbstractProjectEventJob job) {
        job.setUser(true);
        job.schedule();
    }
}
