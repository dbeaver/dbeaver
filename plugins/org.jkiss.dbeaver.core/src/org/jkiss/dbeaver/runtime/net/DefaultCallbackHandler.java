/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.net;

import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * Global DefaultCallbackHandler
 */
public class DefaultCallbackHandler implements CallbackHandler {

    private char[] password = null;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof PasswordCallback) {
                if (password == null) {
                    final DBAAuthInfo authInfo = DBUserInterface.getInstance().promptUserCredentials("Enter password", null, null, true);
                    if (authInfo != null) {
                        if (authInfo.isSavePassword()) {
                            password = authInfo.getUserPassword().toCharArray();
                        }
                        ((PasswordCallback) callback).setPassword(authInfo.getUserPassword().toCharArray());
                    }
                } else {
                    ((PasswordCallback) callback).setPassword(password);
                }
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
