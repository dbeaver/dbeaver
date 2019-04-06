/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

public class ConsoleUserInterface implements DBPPlatformUI {
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
    public DBAAuthInfo promptUserCredentials(String prompt, String userName, String userPassword, boolean passwordOnly, boolean showSavePassword) {
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
    public DBNNode selectObject(Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes) {
        return null;
    }

    @Override
    public void openEntityEditor(DBSObject object) {
        throw new IllegalStateException("Editors not supported in console mode");
    }

    @Override
    public void openEntityEditor(DBNNode selectedNode, String defaultPageId) {
        throw new IllegalStateException("Editors not supported in console mode");
    }

    @Override
    public void openConnectionEditor(DBPDataSourceContainer dataSourceContainer) {
        // do nothing
    }

    @Override
    public void executeProcess(DBRProcessDescriptor processDescriptor) {
        try {
            processDescriptor.execute();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Execute process", processDescriptor.getName(), e);
        }
    }

    @Override
    public void executeInUI(Runnable runnable) {
        runnable.run();
    }

    @Override
    public <RESULT> Job createLoadingService(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer) {
        throw new IllegalStateException("Loading jobs not supported in console mode");
    }

    @Override
    public void refreshPartState(Object part) {
        // do nothing
    }
}
