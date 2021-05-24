/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;

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
        IGNORE_ALL,
        STOP,
        RETRY,
    }

    UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final IStatus status);
    UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final Throwable e);
    UserResponse showError(@NotNull final String title, @Nullable final String message);
    void showMessageBox(@NotNull final String title, @Nullable final String message, boolean error);
    void showWarningMessageBox(@NotNull final String title, @Nullable final String message);
    boolean confirmAction(String title, String message);

    UserResponse showErrorStopRetryIgnore(String task, Throwable error, boolean queue);

    /**
     * Notification agent
     */
    long getLongOperationTimeout();
    void notifyAgent(String message, int status);

    /**
     * Asks for user credentials. Returns null if user canceled this action.
     */
    DBPAuthInfo promptUserCredentials(String prompt, String userName, String userPassword, boolean passwordOnly, boolean showSavePassword);

    DBPAuthInfo promptUserCredentials(String prompt, String userNameLabel, String userName, String passwordLabel, String userPassword, boolean passwordOnly, boolean showSavePassword);

    /**
     * Asks for password change. Returns null if user canceled this action.
     */
    DBAPasswordChangeInfo promptUserPasswordChange(String prompt, @Nullable String userName, @Nullable String oldPassword, boolean userEditable, boolean oldPasswordVisible);

    /**
     * Ask user to accept license agreement
     */
    boolean acceptLicense(String message, String licenseText);

    boolean downloadDriverFiles(DBPDriver driverDescriptor, DBPDriverDependencies dependencies);

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
    void executeWithProgress(@NotNull Runnable runnable);

    void executeWithProgress(@NotNull DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException;

    @NotNull
    <RESULT> Job createLoadingService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer);

    /**
     * FIXME: this is a hack. We need to call platform (workbench) to refresh part's contexts (enabled commands).
     * There is no such thing as part in abstract UI. Need some better solution.
     */
    void refreshPartState(Object part);

    void copyTextToClipboard(String text, boolean htmlFormat);

    void executeShellProgram(String shellCommand);

    boolean readAndDispatchEvents();

}