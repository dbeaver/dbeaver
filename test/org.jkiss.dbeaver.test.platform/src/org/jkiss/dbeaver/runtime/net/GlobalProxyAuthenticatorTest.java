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
package org.jkiss.dbeaver.runtime.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for dbeaver/dbeaver#40874 — driver proxy username and
 * password could not be cleared from the preference page. The previous
 * {@code saveCredentials} implementation short-circuited on an empty
 * username or password, leaving the stale secret on disk and causing
 * the UI to re-populate the old values on reopen.
 */
public class GlobalProxyAuthenticatorTest extends DBeaverUnitTest {

    private RecordingSecretController secrets;

    @Before
    public void setUp() {
        secrets = new RecordingSecretController();
    }

    @Test
    public void savingNonEmptyCredentialsStoresBothValues() throws DBException {
        GlobalProxyAuthenticator.saveCredentials(secrets, "alice", "s3cret");

        assertEquals("alice", secrets.store.get(ModelPreferences.UI_PROXY_USER));
        assertEquals("s3cret", secrets.store.get(ModelPreferences.UI_PROXY_PASSWORD));
        assertTrue("flushChanges must be called so the values hit disk", secrets.flushed);
    }

    @Test
    public void savingEmptyUsernameAndPasswordClearsBothEntries() throws DBException {
        // Pre-existing saved credentials — this is the state the reporter
        // is in after entering username + password once.
        secrets.store.put(ModelPreferences.UI_PROXY_USER, "alice");
        secrets.store.put(ModelPreferences.UI_PROXY_PASSWORD, "s3cret");
        secrets.flushed = false;

        // User empties both fields and clicks Apply.
        GlobalProxyAuthenticator.saveCredentials(secrets, "", "");

        // Previously the method skipped the write entirely; now both
        // entries are cleared (set to null so LocalSecretController
        // resolves the preference to its default / empty state).
        assertFalse(
            "empty username must clear the stored proxy user",
            secrets.store.containsKey(ModelPreferences.UI_PROXY_USER));
        assertFalse(
            "empty password must clear the stored proxy password",
            secrets.store.containsKey(ModelPreferences.UI_PROXY_PASSWORD));
        assertTrue(secrets.flushed);
    }

    @Test
    public void clearingOnlyPasswordLeavesUsernameStored() throws DBException {
        secrets.store.put(ModelPreferences.UI_PROXY_USER, "alice");
        secrets.store.put(ModelPreferences.UI_PROXY_PASSWORD, "s3cret");

        GlobalProxyAuthenticator.saveCredentials(secrets, "alice", "");

        assertEquals("alice", secrets.store.get(ModelPreferences.UI_PROXY_USER));
        assertFalse(
            "empty password must clear the stored proxy password even when the username is still set",
            secrets.store.containsKey(ModelPreferences.UI_PROXY_PASSWORD));
    }

    @Test
    public void clearingOnlyUsernameLeavesPasswordStoredWhenReplaced() throws DBException {
        secrets.store.put(ModelPreferences.UI_PROXY_USER, "alice");
        secrets.store.put(ModelPreferences.UI_PROXY_PASSWORD, "s3cret");

        // User blanks the username but re-enters the password — the
        // cleared username must still take effect.
        GlobalProxyAuthenticator.saveCredentials(secrets, "", "s3cret");

        assertFalse(
            "empty username must clear the stored proxy user even when the password is still set",
            secrets.store.containsKey(ModelPreferences.UI_PROXY_USER));
        assertEquals("s3cret", secrets.store.get(ModelPreferences.UI_PROXY_PASSWORD));
    }

    /**
     * Minimal in-memory {@link DBSSecretController} that mirrors
     * {@code LocalSecretController}'s semantics: setting a null value
     * removes the preference, and reading back a removed key returns
     * null.
     */
    private static final class RecordingSecretController implements DBSSecretController {
        final Map<String, String> store = new HashMap<>();
        boolean flushed = false;

        @Nullable
        @Override
        public String getPrivateSecretValue(@NotNull String secretId) {
            return store.get(secretId);
        }

        @Override
        public void setPrivateSecretValue(@NotNull String secretId, @Nullable String secretValue) {
            if (secretValue == null) {
                store.remove(secretId);
            } else {
                store.put(secretId, secretValue);
            }
        }

        @NotNull
        @Override
        public List<DBSSecretValue> discoverCurrentUserSecrets(@NotNull DBSSecretObject secretObject) {
            return List.of();
        }

        @Override
        public void flushChanges() {
            flushed = true;
        }
    }
}
