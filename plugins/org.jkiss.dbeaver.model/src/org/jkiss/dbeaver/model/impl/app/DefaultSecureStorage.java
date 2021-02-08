/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.runtime.encode.ContentEncrypter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * DefaultSecureStorage
 */
public class DefaultSecureStorage implements DBASecureStorage {

    private static final byte[] LOCAL_KEY_CACHE = new byte[] { -70, -69, 74, -97, 119, 74, -72, 83, -55, 108, 45, 101, 61, -2, 84, 74 };
    
    public static DefaultSecureStorage INSTANCE = new DefaultSecureStorage();

    @Override
    public boolean useSecurePreferences() {
        return false;
    }

    @Override
    public ISecurePreferences getSecurePreferences() {
        return SecurePreferencesFactory.getDefault().node("dbeaver");
    }

    @Override
    public SecretKey getLocalSecretKey() {
        return new SecretKeySpec(LOCAL_KEY_CACHE, ContentEncrypter.KEY_ALGORITHM);
    }

}