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
package org.jkiss.dbeaver.model.security.linux;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;
import org.jkiss.code.NotNull;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.spec.PBEKeySpec;

/** Provides the Equinox encryption password from the Linux desktop keyring. */
public class LinuxPasswordProvider extends PasswordProvider implements IValidatingPasswordProvider {
    private static final int PASSWORD_LENGTH = 64;

    @Override
    public boolean isValid() {
        return Platform.OS_LINUX.equals(Platform.getOS()) && LinuxKeyring.isAvailable();
    }

    @NotNull
    @Override
    public synchronized PBEKeySpec getPassword(@NotNull IPreferencesContainer container, int passwordType) {
        // One keyring password can encrypt more than one Equinox storage file.
        if ((passwordType & PASSWORD_CHANGE) != 0) {
            throw new UnsupportedOperationException("Changing the Linux keyring encryption password is not supported");
        }
        byte[] password = LinuxKeyring.getPassword();
        if (password == null && (passwordType & CREATE_NEW_PASSWORD) == 0) {
            throw new IllegalStateException("Linux keyring encryption password is missing");
        }
        if (password == null) {
            byte[] random = new byte[PASSWORD_LENGTH];
            new SecureRandom().nextBytes(random);
            try {
                password = Base64.getEncoder().encode(random);
            } finally {
                Arrays.fill(random, (byte) 0);
            }
            try {
                password = LinuxKeyring.addPassword(password);
            } catch (RuntimeException e) {
                Arrays.fill(password, (byte) 0);
                throw e;
            }
        }
        char[] characters = new char[password.length];
        try {
            for (int i = 0; i < password.length; i++) {
                characters[i] = (char) (password[i] & 0xff);
            }
            return new PBEKeySpec(characters);
        } finally {
            Arrays.fill(characters, '\0');
            Arrays.fill(password, (byte) 0);
        }
    }

    @Override
    public boolean retryOnError(@NotNull Exception e, @NotNull IPreferencesContainer container) {
        return false;
    }
}
