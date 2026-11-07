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
package org.jkiss.dbeaver.ui.datadam.project.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.job.DDProjectUnbindJob;

public class DDProjectUnbindHandler extends DDProjectHandler<DDProjectUnbindJob> {
    @Nullable
    @Override
    protected DDProjectUnbindJob createJob(@NotNull ExecutionEvent event, @NotNull DBPProject project) {
        if (!UIUtils.confirmAction(
            HandlerUtil.getActiveShell(event),
            DDTrackingUIMessages.project_sync_unbind_confirm_title,
            NLS.bind(DDTrackingUIMessages.project_sync_unbind_confirm_message, project.getName()))) {
            return null;
        }
        return new DDProjectUnbindJob(project);
    }

    @Override
    protected void onSuccess(@NotNull DDProjectUnbindJob job) {
        showMessage(DDTrackingUIMessages.project_sync_unbind_success);
    }
}
