/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * SSH utils
 */
public class SSHUtils {

    private static final Log log = Log.getLog(SSHUtils.class);

    private static final String PLATFORM_SSH_PREFERENCES_NODE = "org.eclipse.jsch.core"; //$NON-NLS-1$
    private static final String PLATFORM_SSH_PREFERENCES_SSH2HOME_KEY = IConstants.KEY_SSH2HOME;
    private static final String DEFAULT_SSH_HOME_DIR_NAME = IConstants.SSH_DEFAULT_HOME;
    private static final String DEFAULT_SSH_HOME_DIR_NAME_WIN_OLD = IConstants.SSH_OLD_DEFAULT_WIN32_HOME;
    private static final String KNOWN_SSH_HOSTS_FILE_NAME = "known_hosts";

    static int findFreePort() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        int minPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MIN);
        int maxPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MAX);
        return IOUtils.findFreePort(minPort, maxPort);
    }

    public static boolean isKeyFileEncrypted(String privKeyPath) {
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


    public static boolean isKeyEncrypted(byte[] privKeyValue) {
        // Check whether this key is encrypted
        if (privKeyValue != null) {
            try {
                JSch testSch = new JSch();
                KeyPair keyPair = KeyPair.load(testSch, privKeyValue, null);
                return keyPair.isEncrypted();
            } catch (JSchException e) {
                // Something went wrong
                log.debug("Can't check private key encryption: " + e.getMessage());
            }
        }
        return false;
    }


    @NotNull
    public static File getKnownSshHostsFileOrDefault() {
        return  getKnownSshHostsFileImpl(true);
    }

    @Nullable
    public static File getKnownSshHostsFileOrNull() {
        return  getKnownSshHostsFileImpl(false);
    }

    private static File getKnownSshHostsFileImpl(boolean forceFileObjectOnFail) {
        String sshHomePathString = Platform.getPreferencesService().getString(PLATFORM_SSH_PREFERENCES_NODE, PLATFORM_SSH_PREFERENCES_SSH2HOME_KEY, null, null);
        if (CommonUtils.isNotEmpty(sshHomePathString)) {
            try {
                return Paths.get(sshHomePathString, KNOWN_SSH_HOSTS_FILE_NAME).toFile();
            } catch (InvalidPathException e) {
                log.warn("Failed to resolve SSH known hosts file location at " + sshHomePathString, e);
                if (forceFileObjectOnFail) {
                    return new File(sshHomePathString + File.pathSeparator + KNOWN_SSH_HOSTS_FILE_NAME);
                } else {
                    return null;
                }
            }
        } else {
            // seems preference path not set at all, so try to resolve it and preserve
            return resolveDefaultKnownSshHostsFile(forceFileObjectOnFail, true);
        }
    }

    private static File resolveDefaultKnownSshHostsFile(boolean forceFileObjectOnFail, boolean updatePreferences) {
        try {
            String userHomePathString = System.getProperty(IConstants.SYSTEM_PROPERTY_USER_HOME);
            if (userHomePathString != null) {
                Path userHomeDirPath = Paths.get(userHomePathString);
                if (Files.isDirectory(userHomeDirPath)) {
                    Path sshHomeDirPath = userHomeDirPath.resolve(DEFAULT_SSH_HOME_DIR_NAME);

                    if (RuntimeUtils.isWindows() && (!Files.isDirectory(sshHomeDirPath) || Files.notExists(sshHomeDirPath))) {
                        Path sshHomeOldDirPath = userHomeDirPath.resolve(DEFAULT_SSH_HOME_DIR_NAME_WIN_OLD);
                        if (Files.isDirectory(sshHomeOldDirPath)) {
                            sshHomeDirPath = sshHomeOldDirPath;
                        }
                    }

                    if (Files.isDirectory(sshHomeDirPath) || Files.notExists(sshHomeDirPath)) {
                        // don't need to create it until we'll need to write known hosts file

                        if (updatePreferences) {
                            Platform.getPreferencesService()
                                .getRootNode().node(PLATFORM_SSH_PREFERENCES_NODE)
                                .put(PLATFORM_SSH_PREFERENCES_SSH2HOME_KEY, sshHomeDirPath.toAbsolutePath().toString());
                        }

                        return sshHomeDirPath.resolve(KNOWN_SSH_HOSTS_FILE_NAME).toFile();
                    } else {
                        log.warn("Failed to resolve default SSH known hosts file location due to invalid SSH home directory " + sshHomeDirPath.toAbsolutePath());
                    }
                } else {
                    log.warn("Failed to resolve default SSH known hosts file location due to missing user home directory " + userHomeDirPath.toAbsolutePath());
                }
            } else {
                log.warn("Failed to resolve default SSH known hosts file location due to missing user home system property.");
            }
        } catch (Throwable e) {
            log.warn("Failed to resolve default SSH known hosts file location.", e);
        }

        if (forceFileObjectOnFail) {
            Path forcedUserProfilePath = Paths.get(RuntimeUtils.isWindows() ? "%USERPROFILE%" : "~"); // let's pretend it'll resolve itself
            return forcedUserProfilePath.resolve(DEFAULT_SSH_HOME_DIR_NAME).resolve(KNOWN_SSH_HOSTS_FILE_NAME).toFile();
        } else {
            return null;
        }
    }

    public static void forcePlatformReloadKnownHostsPreferences() {
        JSchCorePlugin.getPlugin().setNeedToLoadKnownHosts(true);
        JSchCorePlugin.getPlugin().getJSch().setHostKeyRepository(null);
    }

}
