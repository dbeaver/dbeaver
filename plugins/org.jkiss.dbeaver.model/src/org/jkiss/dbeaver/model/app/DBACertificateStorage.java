/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Certificate storage
 */
public interface DBACertificateStorage
{
    KeyStore getKeyStore(String ksId) throws DBException;

    void addCertificate(String ksId, String certId, InputStream certStream) throws DBException;

    void deleteCertificate(String ksId, String certId) throws DBException;

    File getKeyStorePath(String ksId);
}
