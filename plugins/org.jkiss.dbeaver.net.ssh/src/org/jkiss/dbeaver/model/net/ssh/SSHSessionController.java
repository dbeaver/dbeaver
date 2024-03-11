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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * SSH session controller
 * <p><b>Usage Examples:</b>
 * <pre>
 *   SSHSessionController controller = ...
 *
 *   // create a simple tunnel to the specified host
 *   var host = new SSHHostConfiguration("user", "host", 22, ...);
 *   var session = controller.acquireSession(monitor, configuration, host, null, null);
 *
 *   // create a tunnel with a jump host in between
 *   var jumpHost = new SSHHostConfiguration("user", "jump-host", 22, ...);
 *   var destHost = new SSHHostConfiguration("user", "host", 22, ...);
 *   var jumpSession = controller.acquireSession(monitor, configuration, jumpHost, null, null);
 *   var destSession = controller.acquireSession(monitor, configuration, destHost, jumpSession, null);
 *
 *   // create a tunnel with port forwarding (remote :5678 -> local :1234)
 *   var host = new SSHHostConfiguration("user", "host", 22, ...);
 *   var portForward = new SSHPortForwardConfiguration("localhost", 1234, "", 5678);
 *   var session = controller.acquireSession(monitor, configuration, host, null, null);
 * </pre>
 *
 * @see SSHSession
 */
public interface SSHSessionController {
    /**
     * Acquires SSH session that is connected to the specified destination.
     *
     * @param monitor       progress monitor
     * @param configuration client configuration
     * @param destination   destination host configuration
     * @param origin        origin session (optional). If specified, the session will be created as a nested
     *                      session of the origin session. This is useful for creating "jump" sessions.
     * @param portForward   port forward configuration (optional). May not be required if the session is a nested session.
     * @return SSH session
     * @throws DBException on any error
     */
    @NotNull
    SSHSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException;

    void invalidate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBCInvalidatePhase phase,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException;

    void release(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException;

    /**
     * Returns a list of shared sessions. A shared session is a session that can be used for multiple connections.
     */
    @NotNull
    SSHSession[] getSessions();

    @NotNull
    DBPDataSourceContainer[] getDependentDataSources(@NotNull SSHSession session);
}
