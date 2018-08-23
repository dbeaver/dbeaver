/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;

import java.security.Provider;
import java.security.Security;

/**
 * Bouncy Castle linker
 */
public class SecurityProviderUtils {
    private static final Log log = Log.getLog(SecurityProviderUtils.class);

    public static final String BC_SECURITY_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    private static String securityProvider = null;
    private static boolean registrationDone;

    public static void registerSecurityProvider() {
        if (!registrationDone) {
            try {
                if (securityProvider == null) {
                    registerSecurityProvider(BC_SECURITY_PROVIDER_CLASS);
                    if (securityProvider == null) {
                        log.info("BouncyCastle not registered, using the default JCE provider");
                    }
                }
            } finally {
                registrationDone = true;
            }
        }
    }

    private static boolean registerSecurityProvider(String providerClassName) {
        Provider provider = null;
        try {
            Class<?> name = Class.forName(providerClassName);
            provider = (Provider) name.newInstance();
        } catch (Exception e) {
            log.debug("Can't find BC security provider. Use default JCE.");
        }

        if (provider == null) {
            return false;
        }

        try {
            if (Security.getProvider(provider.getName()) == null) {
                Security.addProvider(provider);
            }

            if (securityProvider == null) {
                securityProvider = provider.getName();
                log.debug("BounceCastle bundle found. Use JCE provider " + provider.getName());
                return true;
            }
        } catch (Exception e) {
            log.warn("Registration of Security Provider '" + providerClassName + "' unexpectedly failed", e);
        }
        return false;
    }


}
