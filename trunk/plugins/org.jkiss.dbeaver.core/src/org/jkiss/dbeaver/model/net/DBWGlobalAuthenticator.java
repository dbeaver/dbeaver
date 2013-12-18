/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.net;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Global authenticator
 */
public class DBWGlobalAuthenticator extends Authenticator {

    private SecuredPasswordEncrypter encrypter;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        final String proxyHost = store.getString(PrefConstants.UI_PROXY_HOST);
        if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
            store.getInt(PrefConstants.UI_PROXY_PORT) == getRequestingPort())
        {
            String userName = store.getString(PrefConstants.UI_PROXY_USER);
            String userPassword = decryptPassword(store.getString(PrefConstants.UI_PROXY_PASSWORD));
            if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                // Ask user
                final Shell shell = DBeaverUI.getActiveWorkbenchShell();
                final BaseAuthDialog authDialog = new BaseAuthDialog(shell, "Auth proxy '" + proxyHost + "'", DBIcon.CONNECTIONS.getImage());
                authDialog.setUserName(userName);
                authDialog.setUserPassword(userPassword);
                final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
                    @Override
                    public void run()
                    {
                        result = (authDialog.open() == IDialogConstants.OK_ID);
                    }
                };
                UIUtils.runInUI(shell, binder);
                if (binder.getResult() != null && binder.getResult()) {
                    userName = authDialog.getUserName();
                    userPassword = authDialog.getUserPassword();
                    if (authDialog.isSavePassword()) {
                        // Save in preferences
                        store.setValue(PrefConstants.UI_PROXY_USER, userName);
                        store.setValue(PrefConstants.UI_PROXY_PASSWORD, encryptPassword(userPassword));
                    }
                }
            }
            if (!CommonUtils.isEmpty(userName) && !CommonUtils.isEmpty(userPassword)) {
                return new PasswordAuthentication(userName, userPassword.toCharArray());
            }
        }

        return null;
    }

    private String encryptPassword(String password) {
        try {
            if (encrypter == null) {
                encrypter = new SecuredPasswordEncrypter();
            }
            return encrypter.encrypt(password);
        } catch (EncryptionException e) {
            return password;
        }
    }

    private String decryptPassword(String password) {
        if (CommonUtils.isEmpty(password)) {
            return password;
        }
        try {
            if (encrypter == null) {
                encrypter = new SecuredPasswordEncrypter();
            }
            return encrypter.decrypt(password);
        } catch (EncryptionException e) {
            return password;
        }
    }

}
