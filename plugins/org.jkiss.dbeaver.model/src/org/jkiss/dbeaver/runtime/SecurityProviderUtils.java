/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

    // Classic BC provider
    public static final String BC_PROVIDER_JCE = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    // FIPS provider
    public static final String BC_PROVIDER_JCAJCE = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";

    public static final String[] BC_PROVIDER_CLASSES = {
        BC_PROVIDER_JCE,
        BC_PROVIDER_JCAJCE
    };

    private static Provider securityProvider = null;
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
            Provider provider = null;
            for (String providerClass : BC_PROVIDER_CLASSES) {
                try {
                    provider = (Provider) Class.forName(providerClass).getConstructor().newInstance();
                } catch (Throwable e) {
                    // ignore
                }
            }
            if (provider == null) {
                log.debug("No BC security providers were found");
                return false;
            }

            if (Security.getProvider(provider.getName()) == null) {
                Security.addProvider(provider);
            }

            if (securityProvider == null) {
                securityProvider = provider;
                log.debug("BounceCastle bundle found. Use JCE provider " + provider.getName());
                return true;
            }
        } catch (Exception e) {
            log.warn("Registration of BC Security Provider unexpectedly failed", e);
        }
        return false;
    }

    public static Provider getActiveSecurityProvider() {
        return securityProvider;
    }

    public static String getActiveSecurityProviderClass() {
        return securityProvider == null ? null : securityProvider.getName();
    }

}
