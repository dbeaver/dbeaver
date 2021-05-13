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

package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.io.File;
import java.security.KeyStore;

/**
 * Certificate storage
 */
public interface DBACertificateStorage
{
    void addCertificate(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull String certType,
        @Nullable byte[] caCertStream,
        @Nullable byte[] clientCertStream,
        @Nullable byte[] keyStream) throws DBException;

    void addCertificate(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull String certType,
        @NotNull String keyStorePath,
        @NotNull char[] keyStorePassword) throws DBException;

    void addSelfSignedCertificate(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull String certType,
        @NotNull String certDN) throws DBException;

    void deleteCertificate(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull String certType) throws DBException;

    KeyStore getKeyStore(DBPDataSourceContainer container, String certType) throws DBException;

    File getKeyStorePath(DBPDataSourceContainer dataSource, String certType);

    @NotNull
    char[] getKeyStorePassword(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType);

    String getKeyStoreType(DBPDataSourceContainer dataSource);
}
