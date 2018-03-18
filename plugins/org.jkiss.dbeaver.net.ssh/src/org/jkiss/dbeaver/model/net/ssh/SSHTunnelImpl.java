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
package org.jkiss.dbeaver.model.net.ssh;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private static final String DEF_IMPLEMENTATION = "jsch";

    private SSHImplementation implementation;

    @Override
    public DBPConnectionConfiguration initializeTunnel(DBRProgressMonitor monitor, DBPPlatform platform, DBWHandlerConfiguration configuration, DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException
    {
        Map<String,String> properties = configuration.getProperties();
        String implId = properties.get(SSHConstants.PROP_IMPLEMENTATION);
        if (CommonUtils.isEmpty(implId)) {
            // Backward compatibility
            implId = DEF_IMPLEMENTATION;
        }

        try {
            SSHImplementationDescriptor implDesc = SSHImplementationRegistry.getInstance().getDescriptor(implId);
            if (implDesc == null) {
                implDesc = SSHImplementationRegistry.getInstance().getDescriptor(DEF_IMPLEMENTATION);
            }
            implementation = implDesc.getImplClass().createInstance(SSHImplementation.class);
        } catch (Throwable e) {
            throw new DBException("Can't create SSH tunnel implementation", e);
        }
        return implementation.initTunnel(monitor, platform, configuration, connectionInfo);
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor) throws DBException, IOException
    {
        if (implementation != null) {
            implementation.closeTunnel(monitor);
            implementation = null;
        }
    }

    @Override
    public AuthCredentials getRequiredCredentials(DBWHandlerConfiguration configuration) {
        if (!configuration.isEnabled() || !configuration.isSecured()) {
            return AuthCredentials.NONE;
        }
        if (configuration.isSavePassword()) {
            return AuthCredentials.NONE;
        }

        String sshAuthType = configuration.getProperties().get(SSHConstants.PROP_AUTH_TYPE);
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        if (sshAuthType != null) {
            authType = SSHConstants.AuthType.valueOf(sshAuthType);
        }
        if (authType == SSHConstants.AuthType.PUBLIC_KEY) {
            // Check whether this key is encrypted
            String privKeyPath = configuration.getProperties().get(SSHConstants.PROP_KEY_PATH);
            if (privKeyPath != null && SSHUtils.isKeyEncrypted(privKeyPath)) {
                return AuthCredentials.PASSWORD;
            }

            return AuthCredentials.NONE;
        }
        return AuthCredentials.CREDENTIALS;
    }

    @Override
    public void invalidateHandler(DBRProgressMonitor monitor) throws DBException, IOException {
        if (implementation != null) {
            implementation.invalidateTunnel(monitor);
        }
    }

}
