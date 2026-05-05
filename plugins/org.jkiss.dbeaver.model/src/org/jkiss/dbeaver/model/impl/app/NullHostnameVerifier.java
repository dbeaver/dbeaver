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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.dbeaver.Log;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
 * Class used for skipping host verification for SSL
 */
public class NullHostnameVerifier implements HostnameVerifier {
    public static final NullHostnameVerifier INSTANCE = new NullHostnameVerifier();

    private static final Log log = Log.getLog(NullHostnameVerifier.class);

    private NullHostnameVerifier() {
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        log.debug("Skipping host verification for domain " + hostname);
        return true;
    }

}

