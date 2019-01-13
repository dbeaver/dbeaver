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
package org.jkiss.dbeaver.runtime.net;

import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.runtime.DBWorkbench;

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
                    final DBAAuthInfo authInfo = DBWorkbench.getPlatformUI().promptUserCredentials("Enter password", null, null, true, true);
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
