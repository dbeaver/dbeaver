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
package org.jkiss.dbeaver.model.security;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class TrustStoreUtils {
    // FIXME: Not secure at all!!!
    // However some people need to use self-signed and untrusted server.
    // Crap.
    public static final X509TrustManager[] NON_VALIDATING_TRUST_MANAGERS = new X509TrustManager[] {
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            public void checkClientTrusted(
                X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
                X509Certificate[] certs, String authType) {
            }
        }
    };
}
