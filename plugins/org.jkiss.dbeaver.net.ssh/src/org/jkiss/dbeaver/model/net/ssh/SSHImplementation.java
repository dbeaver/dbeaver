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
package org.jkiss.dbeaver.model.net.ssh;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SSH tunnel implementation
 */
public interface SSHImplementation {

    DBPConnectionConfiguration initTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException;

    String getClientVersion();

    String getServerVersion();

    void invalidateTunnel(DBRProgressMonitor monitor)
        throws DBException, IOException;

    void closeTunnel(DBRProgressMonitor monitor)
        throws DBException, IOException;

    /**
     * Downloads a file from the specified remote path.
     *
     * @param src     path to a remote file
     * @param dst     output stream used for writing data to
     * @param monitor progress monitor which is used for tracking download progress
     */
    void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException;

    /**
     * Uploads a file to the specified remote path.
     *
     * @param src     input stream used for reading data from
     * @param dst     path to a remote file
     * @param monitor progress monitor which is used for tracking upload progress
     */
    void putFile(
        @NotNull InputStream src,
        @NotNull String dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException;

}
