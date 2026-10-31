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
package org.jkiss.dbeaver.model.security.macos;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;
import org.jkiss.code.NotNull;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.spec.PBEKeySpec;

/** Provides the password used to encrypt new Equinox secure preferences on macOS. */
public class MacOSPasswordProvider extends PasswordProvider implements IValidatingPasswordProvider {
    private static final int PASSWORD_LENGTH = 64;

    @Override
    public boolean isValid() {
        return Platform.OS_MACOSX.equals(Platform.getOS());
    }

    @Override
    @NotNull
    public PBEKeySpec getPassword(@NotNull IPreferencesContainer container, int passwordType) {
        // The Keychain owns the password, not the secure preferences file. Reuse it even when
        // Equinox creates a new file, or that file's verification token cannot be decrypted.
        if ((passwordType & PASSWORD_CHANGE) != 0) {
            // This key is shared by all secure-preferences files of this user. Rotating it
            // for one file would make the other files unreadable.
            throw new UnsupportedOperationException("Changing the macOS Keychain encryption password is not supported");
        }
        byte[] password = MacOSKeychain.getPassword();
        if (password == null && (passwordType & CREATE_NEW_PASSWORD) == 0) {
            throw new IllegalStateException("macOS Keychain encryption password is missing");
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
                password = MacOSKeychain.addPassword(password);
            } catch (RuntimeException e) {
                Arrays.fill(password, (byte) 0);
                throw e;
            }
        }
        // Never return null on Keychain failure: in headless mode Equinox may then save plaintext.
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
        // An invalid password must not silently replace the key for existing encrypted values.
        return false;
    }
}
