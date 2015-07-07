/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

    private static DBUICallback instance = new DBUICallback() {
        @Override
        public void showError(@NotNull String title, @Nullable String message, @NotNull IStatus status) {
            System.out.println(title + (message == null ? "" : ": " + message));
            printStatus(status, 0);
        }

        @Override
        public void showError(@NotNull String title, @Nullable String message, @NotNull Throwable e) {
            System.out.println(title + (message == null ? "" : ": " + message));
            e.printStackTrace(System.out);
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
        public DBAAuthInfo promptUserCredentials(String prompt, String userName, String userPassword) {
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
    };

    public static DBUICallback getInstance() {
        return instance;
    }

    public static void setInstance(DBUICallback instance) {
        DBUserInterface.instance = instance;
    }
}