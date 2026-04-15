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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/***
 *
 * Credentials provider interface. Implementations of this interface are responsible for providing credentials to AI engines.
 * They can retrieve credentials from various sources
 * Not all engines have multiple credentials providers, but if an engine supports multiple providers, it should use this interface to retrieve credentials.
 *
 */
public interface AICredentialsProvider<CREDENTIALS> {

    /**
     * Returns the unique ID of the credentials provider. This ID is used to identify the provider and should be unique across all providers.
     *
     * @return the unique ID of the credentials provider
     */
    @NotNull
    String getProviderId();

    /**
     * Resolves secrets and returns credentials. This method is called by the engine to retrieve credentials when needed.
     * Implementations should handle any necessary logic to retrieve and resolve secrets, such as decrypting stored credentials or fetching them from a secure vault.
     *
     * @return the resolved credentials
     * @throws DBException if there is an error while resolving secrets or retrieving credentials
     */
    @Nullable
    CREDENTIALS resolveSecrets() throws DBException;

    /**
     * Returns the currently stored credentials without resolving secrets. This method can be used to retrieve the raw credentials,
     * which may contain unresolved secrets or placeholders.
     * @return the currently stored credentials without resolving secrets
     */
    @Nullable
    CREDENTIALS getCredentials();

    /**
     * Checks if the credentials provided by this provider are valid. This method can be used to verify that the credentials are correctly configured and can be used to authenticate with the AI engine.
     *
     * @return true if the credentials are valid, false otherwise
     */
    boolean isValid();
}
