/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;

/**
 * User interface interactions
 */
public class DBUserInterface {

    private static DBPPlatformUI instance = new DBPPlatformUI() {
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
        public DBAAuthInfo promptUserCredentials(String prompt, String userName, String userPassword, boolean passwordOnly) {
            return null;
        }

        @Override
        public void executeProcess(DBRProcessDescriptor processDescriptor) {
            try {
                processDescriptor.execute();
            } catch (DBException e) {
                DBUserInterface.getInstance().showError("Execute process", processDescriptor.getName(), e);
            }
        }

        @Override
        public void executeInUI(Runnable runnable) {
            runnable.run();
        }
    };

    public static DBPPlatformUI getInstance() {
        return instance;
    }

    public static void setInstance(DBPPlatformUI instance) {
        DBUserInterface.instance = instance;
    }

}