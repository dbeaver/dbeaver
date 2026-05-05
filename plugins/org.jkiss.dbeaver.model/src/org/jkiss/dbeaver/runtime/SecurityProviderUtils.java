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
package org.jkiss.dbeaver.runtime;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jkiss.dbeaver.Log;

import java.security.Provider;
import java.security.Security;

/**
 * Bouncy Castle linker
 */
public class SecurityProviderUtils {
    private static final Log log = Log.getLog(SecurityProviderUtils.class);

    private static String securityProvider = null;
    private static boolean registrationDone;

    public static void registerSecurityProvider() {
        if (!registrationDone) {
            try {
                if (securityProvider == null) {
                    registerBouncyCastleSecurityProvider();
                    if (securityProvider == null) {
                        log.debug("BouncyCastle not registered, using the default JCE provider");
                    }
                }
            } finally {
                registrationDone = true;
            }
        }
    }

    private static boolean registerBouncyCastleSecurityProvider() {
        try {
            Provider provider = new BouncyCastleProvider();

            if (Security.getProvider(provider.getName()) == null) {
                Security.addProvider(provider);
            }

            if (securityProvider == null) {
                securityProvider = provider.getName();
                log.debug("BounceCastle bundle found. Use JCE provider " + provider.getName());
                return true;
            }
        } catch (Exception e) {
            log.warn("Registration of BC Security Provider unexpectedly failed", e);
        }
        return false;
    }


}
