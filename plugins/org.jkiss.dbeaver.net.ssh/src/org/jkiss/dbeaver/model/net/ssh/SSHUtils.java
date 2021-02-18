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
package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.IOUtils;

import java.util.List;

/**
 * SSH utils
 */
class SSHUtils {

    private static final Log log = Log.getLog(SSHUtils.class);

    static int findFreePort(DBPPlatform platform)
    {
        DBPPreferenceStore store = platform.getPreferenceStore();
        int minPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MIN);
        int maxPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MAX);
        return IOUtils.findFreePort(minPort, maxPort);
    }

    public static boolean isKeyEncrypted(String privKeyPath) {
            // Check whether this key is encrypted
        if (privKeyPath != null) {
            // Determine whether public key is encrypted
            try {
                JSch testSch = new JSch();
                testSch.addIdentity(privKeyPath);
                IdentityRepository ir = testSch.getIdentityRepository();
                List<Identity> identities = ir.getIdentities();
                for (Identity identity : identities) {
                    if (identity.isEncrypted()) {
                        return true;
                    }
                }
            } catch (JSchException e) {
                // Something went wrong
                log.debug("Can't check private key encryption: " + e.getMessage());
            }
        }
        return false;
    }
}
