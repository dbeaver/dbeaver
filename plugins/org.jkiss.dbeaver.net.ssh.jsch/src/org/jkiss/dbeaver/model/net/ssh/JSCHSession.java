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

import com.jcraft.jsch.*;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JSCHSession extends AbstractSession {
    private final JSCHSessionController controller;
    private Session session;

    public JSCHSession(@NotNull JSCHSessionController controller) {
        this.controller = controller;
    }

    @Override
    public void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        session = controller.createNewSession(monitor, configuration, destination);
    }

    @Override
    public void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) {
        // FIXME: timeout is not used
        session.disconnect();
        session = null;
    }

    @NotNull
    @Override
    public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        try {
            final int port = session.setPortForwardingL(
                configuration.localHost(),
                configuration.localPort(),
                configuration.remoteHost(),
                configuration.remotePort()
            );

            return new SSHPortForwardConfiguration(
                configuration.localHost(),
                port,
                configuration.remoteHost(),
                configuration.remotePort()
            );
        } catch (JSchException e) {
            throw new DBException("Unable to set up port forwarding", e);
        }
    }

    @Override
    public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        if (!session.isConnected()) {
            // The connection might already have been closed due to a network error
            return;
        }
        try {
            session.delPortForwardingL(configuration.localHost(), configuration.localPort());
        } catch (JSchException e) {
            throw new DBException("Unable to remove port forwarding", e);
        }
    }

    @Override
    public void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws IOException {
        final ChannelSftp channel = openSftpChannel();

        try {
            channel.get(src, dst, new SftpProgressMonitorAdapter(monitor));
        } catch (SftpException e) {
            throw new IOException("Error downloading file through SFTP channel", e);
        } finally {
            channel.disconnect();
        }
    }

    @Override
    public void putFile(
        @NotNull InputStream src,
        @NotNull String dst,
        @NotNull DBRProgressMonitor monitor
    ) throws IOException {
        final ChannelSftp channel = openSftpChannel();

        try {
            channel.put(src, dst, new SftpProgressMonitorAdapter(monitor));
        } catch (SftpException e) {
            throw new IOException("Error uploading file through SFTP channel", e);
        } finally {
            channel.disconnect();
        }
    }

    @NotNull
    @Override
    public String getClientVersion() {
        return session.getClientVersion();
    }

    @NotNull
    @Override
    public String getServerVersion() {
        return session.getServerVersion();
    }

    @NotNull
    private ChannelSftp openSftpChannel() throws IOException {
        final ChannelSftp channel;

        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
        } catch (JSchException e) {
            throw new IOException("Error opening SFTP channel", e);
        }

        return channel;
    }

    private record SftpProgressMonitorAdapter(@NotNull DBRProgressMonitor delegate) implements SftpProgressMonitor {
        @Override
        public void init(int op, String src, String dst, long max) {
            if (op == PUT) {
                delegate.beginTask(NLS.bind("Upload file ''{0}'' -> ''{1}''", src, dst), (int) max);
            } else {
                delegate.beginTask(NLS.bind("Download file ''{0}'' -> ''{1}''", src, dst), (int) max);
            }
        }

        @Override
        public boolean count(long count) {
            delegate.worked((int) count);

            return !delegate.isCanceled();
        }

        @Override
        public void end() {
            delegate.done();
        }
    }
}
