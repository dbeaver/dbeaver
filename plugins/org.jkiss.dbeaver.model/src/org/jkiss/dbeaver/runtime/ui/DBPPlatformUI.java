/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.runtime.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * User interface interactions
 */
public interface DBPPlatformUI {

    /**
     * Error boxes
     */

    enum UserResponse {
        OK,
        CANCEL,
        IGNORE,
        STOP,
        RETRY,
    }

    UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final IStatus status);
    UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final Throwable e);
    UserResponse showError(@NotNull final String title, @Nullable final String message);
    void showMessageBox(@NotNull final String title, @Nullable final String message, boolean error);

    /**
     * Notification agent
     */
    long getLongOperationTimeout();
    void notifyAgent(String message, int status);

    /**
     * Asks for user credentials. Returns null if user canceled this action.
     */
    DBAAuthInfo promptUserCredentials(String prompt, String userName, String userPassword, boolean passwordOnly, boolean showSavePassword);

    /**
     * Asks for password change. Returns null if user canceled this action.
     */
    DBAPasswordChangeInfo promptUserPasswordChange(String prompt, @Nullable String userName, @Nullable String oldPassword);

    /**
     * UI utilities
     */
    DBNNode selectObject(@NotNull Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes);

    void openEntityEditor(@NotNull DBSObject object);
    void openEntityEditor(@NotNull DBNNode selectedNode, @Nullable String defaultPageId);

    void openConnectionEditor(@NotNull DBPDataSourceContainer dataSourceContainer);

    // Process execution
    void executeProcess(@NotNull DBRProcessDescriptor processDescriptor);

    // Execute some action in UI thread
    void executeInUI(@NotNull Runnable runnable);

    @NotNull
    <RESULT> Job createLoadingService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer);

    /**
     * FIXME: this is a hack. We need to call platform (workbench) to refresh part's contexts (enabled commands).
     * There is no such thing as part in abstract UI. Need some better solution.
     * @part IWorkbenchPart
     */
    void refreshPartState(Object part);
}