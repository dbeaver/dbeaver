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
package org.jkiss.dbeaver.model.access;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DBAuthUtils {
    private static final Log log = Log.getLog(DBAuthUtils.class);
    private static final String externalAuthSuccessHtml;

    static {
        try (InputStream is = DBAuthUtils.class.getResourceAsStream("external_auth_success.html")) {
            Objects.requireNonNull(is, "external_auth_success not found");
            externalAuthSuccessHtml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     *
     * @param monitor to execute query changing password
     * @param dataSourceContainer for connection configuration using and changing
     * @param passwordChangeManager manage syntax of password changing
     * @return First, it will check the program on the headless mode and returns false if it is in this mode.
     * If not, the dialog for changing the current password will be shown.
     * If the user inputs a new password - the password will be changed in the database.
     * Other cases - false will be returned.
     */
    public static boolean promptAndChangePasswordForCurrentUser(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull DBAUserPasswordManager passwordChangeManager
    ) {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode()) {
            return false;
        }
        DBPConnectionConfiguration connectionInfo = dataSourceContainer.getConnectionConfiguration();
        String oldPassword = connectionInfo.getUserPassword();
        DBPConnectionConfiguration actualConnectionConfiguration = dataSourceContainer.getActualConnectionConfiguration();
        String userName = actualConnectionConfiguration.getUserName();
        if (CommonUtils.isEmpty(userName)) {
            // Look at the actual configuration first, then on connection info
            userName = connectionInfo.getUserName();
        }
        if (CommonUtils.isEmpty(oldPassword)) {
            // Credentials not saved in the connection settings, use actual configuration
            oldPassword = actualConnectionConfiguration.getUserPassword();
        }
        DBAPasswordChangeInfo userPassword = DBWorkbench.getPlatformUI().promptUserPasswordChange(
            ModelMessages.dialog_user_password_change_label,
            userName,
            oldPassword,
            false,
            false);
        if (userPassword != null) {
            String newPassword = userPassword.getNewPassword();
            try {
                passwordChangeManager.changeUserPassword(monitor, userName, newPassword, oldPassword);
                if (DBWorkbench.getPlatformUI().confirmAction(
                    ModelMessages.dialog_user_password_change_question_label,
                    ModelMessages.dialog_user_password_change_question_message)
                ) {
                    actualConnectionConfiguration.setUserPassword(newPassword);
                    connectionInfo.setUserPassword(newPassword);
                    if (!dataSourceContainer.isTemporary()) {
                        dataSourceContainer.persistConfiguration();
                    }
                    return true;
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    ModelMessages.dialog_user_password_change_label,
                    ModelMessages.dialog_user_password_change_error_message + userName, e);
            }
        }
        return false;
    }

    @NotNull
    public static String getExternalBrowserSuccessResponse(@Nullable String providerName) {
        return GeneralUtils.replaceVariables(externalAuthSuccessHtml, name -> {
            var result = switch (name) {
                case "PRODUCT_NAME" -> GeneralUtils.getProductTitle();
                case "AUTH_NAME" -> providerName == null ? "for using database" : "in " + providerName;
                default -> {
                    log.error("Unknown variable: '" + name + "'");
                    yield "";
                }
            };
            return CommonUtils.escapeHtml(result);
        });
    }
}
