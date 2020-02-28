/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.ui.console;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class ConsoleUserInterface implements DBPPlatformUI {
    private static final Log log = Log.getLog(ConsoleUserInterface.class);

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message, @NotNull IStatus status) {
        System.out.println(title + (message == null ? "" : ": " + message));
        printStatus(status, 0);
        return UserResponse.OK;
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message, @NotNull Throwable e) {
        System.out.println(title + (message == null ? "" : ": " + message));
        e.printStackTrace(System.out);
        return UserResponse.OK;
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message) {
        System.out.println(title + (message == null ? "" : ": " + message));
        return UserResponse.OK;
    }

    @Override
    public void showMessageBox(String title, String message, boolean error) {
        System.out.println(title + (message == null ? "" : ": " + message));
    }

    @Override
    public boolean confirmAction(String title, String message) {
        return false;
    }

    @Override
    public UserResponse showErrorStopRetryIgnore(String task, Throwable error, boolean queue) {
        System.out.println(task);
        error.printStackTrace(System.out);
        return UserResponse.IGNORE;
    }

    @Override
    public long getLongOperationTimeout() {
        return 0;
    }

    @Override
    public void notifyAgent(String message, int status) {
        // do nothing
    }

    private void printStatus(@NotNull IStatus status, int level) {
        char[] indent = new char[level * 4];
        for (int i = 0; i < indent.length; i++) indent[i] = ' ';
        if (status.getMessage() != null) {
            System.out.println("" + indent + status.getMessage());
        }
        if (status.getException() != null) {
            status.getException().printStackTrace(System.out);
        }
    }

    @Override
    public DBPAuthInfo promptUserCredentials(String prompt, String userName, String userPassword, boolean passwordOnly, boolean showSavePassword) {
        return null;
    }

    @Override
    public DBAPasswordChangeInfo promptUserPasswordChange(String prompt, String userName, String oldPassword) {
        return null;
    }

    @Override
    public boolean acceptLicense(String message, String licenseText) {
        return true;
    }

    @Override
    public boolean downloadDriverFiles(DBPDriver driverDescriptor, DBPDriverDependencies dependencies) {
        return false;
    }

    @Override
    public DBNNode selectObject(@NotNull Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes) {
        return null;
    }

    @Override
    public void openEntityEditor(@NotNull DBSObject object) {
        // do nothing
    }

    @Override
    public void openEntityEditor(@NotNull DBNNode selectedNode, String defaultPageId) {
        // do nothing
    }

    @Override
    public void openConnectionEditor(@NotNull DBPDataSourceContainer dataSourceContainer) {
        // do nothing
    }

    @Override
    public void executeProcess(@NotNull DBRProcessDescriptor processDescriptor) {
        try {
            processDescriptor.execute();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Execute process", processDescriptor.getName(), e);
        }
    }

    @Override
    public void executeWithProgress(@NotNull Runnable runnable) {
        runnable.run();
    }

    @Override
    public void executeWithProgress(@NotNull DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        runnable.run(new LoggingProgressMonitor());
    }

    @NotNull
    @Override
    public <RESULT> Job createLoadingService(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer) {
        return new AbstractJob(loadingService.getServiceName()) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    RESULT result = loadingService.evaluate(monitor);
                    visualizer.completeLoading(result);
                    return Status.OK_STATUS;
                } catch (InvocationTargetException e) {
                    return GeneralUtils.makeExceptionStatus(e.getTargetException());
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
            }
        };
    }

    @Override
    public void refreshPartState(Object part) {
        // do nothing
    }

    @Override
    public void copyTextToClipboard(String text, boolean htmlFormat) {
        // do nothing
    }

    @Override
    public void executeShellProgram(String shellCommand) {
        File filePath = new File(shellCommand);
        if (filePath.exists() && filePath.isDirectory()) {
            System.out.println("Open directory '" + shellCommand + "'");
            return;
        }
        try {
            Runtime.getRuntime().exec(shellCommand);
        } catch (Exception e) {
            log.error(e);
        }
    }
}
