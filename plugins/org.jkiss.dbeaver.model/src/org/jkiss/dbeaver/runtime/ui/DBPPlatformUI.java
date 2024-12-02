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

package org.jkiss.dbeaver.runtime.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Future;

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
    
    class UserChoiceResponse {
        /**
         * index of the user's choice or out of range value (-1) on dialog failure
         */
        public final int choiceIndex;
        @Nullable
        public final Integer forAllChoiceIndex;

        public UserChoiceResponse(int choiceIndex, @Nullable Integer forAllChoiceIndex) {
            this.choiceIndex = choiceIndex;
            this.forAllChoiceIndex = forAllChoiceIndex;
        }
    }

    UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final IStatus status);
    UserResponse showError(@Nullable final String title, @Nullable final String message, @NotNull final Throwable e);
    UserResponse showError(@NotNull final String title, @Nullable final String message);
    void showNotification(@NotNull final String title, @Nullable final String message, boolean error, @Nullable Runnable feedback);
    void showWarningNotification(@NotNull final String title, @Nullable final String message);
    void showMessageBox(@NotNull final String title, @Nullable final String message, boolean error);
    void showWarningMessageBox(@NotNull final String title, @Nullable final String message);
    boolean confirmAction(String title, String message);
    boolean confirmAction(String title, String message, boolean isWarning);
    boolean confirmAction(@NotNull String title, @NotNull String message, @NotNull String buttonLabel, boolean isWarning);

    /**
     * Show user-choice dialog for a user to mandatory select one of the options described with the labels
     * 
     * @param title Dialog window title
     * @param message Dialog window content explaining the choice in question
     * @param labels Options to choose of
     * @param forAllLabels Options to remember the choice
     * @param previousChoice Previous choice made by the user to be focused initially on dialog show
     * @param defaultChoice Choice to use when the UI platform does not support dialogs
     * @return Description of the choice made by the user
     */
    @NotNull
    UserChoiceResponse showUserChoice(
        @NotNull String title,
        @Nullable String message,
        @NotNull List<String> labels,
        @NotNull List<String> forAllLabels,
        @Nullable Integer previousChoice,
        int defaultChoice
    );

    UserResponse showErrorStopRetryIgnore(String task, Throwable error, boolean queue);

    /**
     * Notification agent
     */
    long getLongOperationTimeout();
    void notifyAgent(String message, int status);

    /**
     * Asks for user credentials. Returns null if user canceled this action.
     */
    @Nullable
    default DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @Nullable String userName,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword
    ) {
        return promptUserCredentials(prompt, prompt, userName, userPassword, passwordOnly, showSavePassword);
    }

    @Nullable
    DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @Nullable String description,
        @Nullable String userName,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword);

    @Nullable
    default DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @NotNull String userNameLabel,
        @Nullable String userName,
        @NotNull String passwordLabel,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword
    ) {
        return promptUserCredentials(prompt, prompt, userNameLabel, userName, passwordLabel, userPassword, passwordOnly, showSavePassword);
    }

    @Nullable
    DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @Nullable String description,
        @NotNull String userNameLabel,
        @Nullable String userName,
        @NotNull String passwordLabel,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword);

    /**
     * Asks for password change. Returns null if user canceled this action.
     */
    DBAPasswordChangeInfo promptUserPasswordChange(String prompt, @Nullable String userName, @Nullable String oldPassword, boolean userEditable, boolean oldPasswordVisible);

    /**
     * Ask user to enter some property value
     */
    String promptProperty(String prompt, String defValue);

    /**
     * Ask user to accept license agreement
     */
    boolean acceptLicense(String message, String licenseText);

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

    /**
     * Execute runnable task synchronously while displaying job indicator if needed
     */
    @NotNull
    <T> Future<T> executeWithProgressBlocking(@NotNull String operationDescription, @NotNull DBRRunnableWithResult<Future<T>> runnable);

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

    void showInSystemExplorer(@NotNull String path);

    DBNPathBase openFileSystemSelector(
        @NotNull String title,
        boolean folder,
        int style,
        boolean binary,
        String[] filterExt,
        String defaultValue);

    boolean readAndDispatchEvents();
}