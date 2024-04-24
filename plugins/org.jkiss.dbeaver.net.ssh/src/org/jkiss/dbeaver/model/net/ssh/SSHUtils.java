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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH utils
 */
public class SSHUtils {

    private static final Log log = Log.getLog(SSHUtils.class);

    public static final boolean DISABLE_SESSION_SHARING = Boolean.getBoolean("dbeaver.ssh.disableSessionSharing");

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

    public static boolean isKeyFileEncrypted(@NotNull Path privKeyPath) {
        return isKeyFileEncrypted(privKeyPath.toAbsolutePath().toString());
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

    /**
     * Loads host configurations for tunneling.
     * <p>
     * There might be multiple hosts available, e.g. primary host and several jump hosts, in the following order:
     * <ol>
     *   <li>Jump host #1</li>
     *   <li>Jump host #2</li>
     *   <li>...</li>
     *   <li>Primary host</li>
     * </ol>
     *
     * @param configuration network handler configuration to read host configuration from
     * @param validate      validate configuration parameters
     * @return array of SSH host configurations
     * @throws DBException if configuration is invalid
     */
    @NotNull
    public static SSHHostConfiguration[] loadHostConfigurations(
        @NotNull DBWHandlerConfiguration configuration,
        boolean validate
    ) throws DBException {
        final List<SSHHostConfiguration> hosts = new ArrayList<>();

        for (int i = 0; i < SSHConstants.MAX_JUMP_SERVERS; i++) {
            // jump hosts, if present
            final String prefix = DataSourceUtils.getJumpServerSettingsPrefix(i);
            if (configuration.getBooleanProperty(prefix + RegistryConstants.ATTR_ENABLED)) {
                hosts.add(loadHostConfiguration(configuration, new ConfigurationKind.JumpHost(prefix, i), validate));
            } else {
                break;
            }
        }

        // primary host
        hosts.add(loadHostConfiguration(configuration, new ConfigurationKind.TargetHost(), validate));

        return hosts.toArray(SSHHostConfiguration[]::new);
    }

    @NotNull
    private static SSHHostConfiguration loadHostConfiguration(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull ConfigurationKind kind,
        boolean validate
    ) throws DBException {
        final String prefix = kind.configurationPrefix();
        final boolean savePassword = configuration.isSavePassword() || kind instanceof ConfigurationKind.JumpHost;
        final String password;
        String username;

        if (prefix.isEmpty()) {
            username = CommonUtils.notEmpty(configuration.getUserName());
            password = CommonUtils.nullIfEmpty(configuration.getPassword());
        } else {
            username = CommonUtils.notEmpty(configuration.getStringProperty(prefix + RegistryConstants.ATTR_NAME));
            password = CommonUtils.nullIfEmpty(configuration.getSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD));
        }

        if (validate && CommonUtils.isEmpty(username)) {
            username = SSHConstants.DEFAULT_USER_NAME;
        }

        final String hostname = CommonUtils.notEmpty(configuration.getStringProperty(prefix + DBWHandlerConfiguration.PROP_HOST));
        if (validate && CommonUtils.isEmpty(hostname)) {
            throw new DBException(kind.formatErrorMessage("hostname is not specified"));
        }

        int port = configuration.getIntProperty(prefix + DBWHandlerConfiguration.PROP_PORT);
        if (port == 0) {
            if (validate) {
                throw new DBException(kind.formatErrorMessage("port is not specified"));
            } else {
                port = SSHConstants.DEFAULT_PORT;
            }
        }

        final SSHConstants.AuthType authType = CommonUtils.valueOf(
            SSHConstants.AuthType.class,
            configuration.getStringProperty(prefix + SSHConstants.PROP_AUTH_TYPE),
            SSHConstants.AuthType.PASSWORD
        );
        final SSHAuthConfiguration auth = switch (authType) {
            case PUBLIC_KEY -> {
                final String path = configuration.getStringProperty(prefix + SSHConstants.PROP_KEY_PATH);
                if (CommonUtils.isEmpty(path)) {
                    String privKeyValue = configuration.getSecureProperty(prefix + SSHConstants.PROP_KEY_VALUE);
                    if (validate && privKeyValue == null) {
                        throw new DBException(kind.formatErrorMessage("private key is not specified"));
                    }
                    yield new SSHAuthConfiguration.KeyData(trimLinesInKeyData(CommonUtils.notEmpty(privKeyValue)), password, savePassword);
                } else {
                    if (validate) {
                        validatePathAndEnsureExists(kind, path);
                    }
                    yield new SSHAuthConfiguration.KeyFile(path, password, savePassword);
                }
            }
            case PASSWORD -> new SSHAuthConfiguration.Password(password, savePassword);
            case AGENT -> new SSHAuthConfiguration.Agent();
        };

        return new SSHHostConfiguration(username, hostname, port, auth);
    }

    /**
     * Trims each lines in provided key data.
     * BouncyCastle doesn't trim the last line of key data from version 1.78,
     * which means that keys that contain leading whitespaces
     * in the last line are invalid.
     */
    @NotNull
    public static String trimLinesInKeyData(@NotNull String keyValue) {
        String[] lines = keyValue.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }
        return String.join("\n", lines);
    }

    public static void saveHostConfigurations(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration[] hosts
    ) {
        for (int i = 0; i < hosts.length; i++) {
            if (i < hosts.length - 1) {
                saveHostConfiguration(configuration, hosts[i], DataSourceUtils.getJumpServerSettingsPrefix(i), true, true);
            } else {
                saveHostConfiguration(configuration, hosts[i], "", false, false);
            }
        }
    }

    private static void saveHostConfiguration(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration host,
        @NotNull String prefix,
        boolean markEnabled,
        boolean forceSavePassword
    ) {
        configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_HOST, host.hostname());
        configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_PORT, host.port());

        if (prefix.isEmpty()) {
            configuration.setUserName(host.username());
        } else {
            configuration.setProperty(prefix + RegistryConstants.ATTR_NAME, host.username());
        }

        if (host.auth() instanceof SSHAuthConfiguration.WithPassword auth) {
            // TODO: For now, we enforce password saving for jump hosts
            final boolean savePassword = forceSavePassword || auth.savePassword();
            if (prefix.isEmpty()) {
                configuration.setSavePassword(savePassword);
                configuration.setPassword(savePassword ? auth.password() : null);
            } else {
                configuration.setSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD, savePassword ? auth.password() : null);
            }
        }

        if (host.auth() instanceof SSHAuthConfiguration.Password) {
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PASSWORD.name());
        } else if (host.auth() instanceof SSHAuthConfiguration.KeyFile auth) {
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY.name());
            configuration.setProperty(prefix + SSHConstants.PROP_KEY_PATH, auth.path());
        } else if (host.auth() instanceof SSHAuthConfiguration.KeyData auth) {
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY.name());
            configuration.setSecureProperty(prefix + SSHConstants.PROP_KEY_VALUE, auth.data());
        } else if (host.auth() instanceof SSHAuthConfiguration.Agent) {
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.AGENT.name());
        }

        if (markEnabled) {
            configuration.setProperty(prefix + RegistryConstants.ATTR_ENABLED, true);
        }
    }

    // Had to extract this code into a separate method to avoid triggering a JDT compiler bug
    // https://github.com/eclipse-jdt/eclipse.jdt.core/issues/1394
    private static void validatePathAndEnsureExists(@NotNull ConfigurationKind kind, @NotNull String string) throws DBException {
        final Path path;
        try {
            path = Path.of(string);
        } catch (InvalidPathException e) {
            throw new DBException(kind.formatErrorMessage("invalid private key path: " + string));
        }
        if (Files.notExists(path)) {
            throw new DBException(kind.formatErrorMessage("private key file does not exist: " + string));
        }
    }

    private sealed interface ConfigurationKind {
        @NotNull
        String configurationPrefix();

        @NotNull
        String formatErrorMessage(@NotNull String message);

        record TargetHost() implements ConfigurationKind {
            @NotNull
            @Override
            public String configurationPrefix() {
                return "";
            }

            @NotNull
            @Override
            public String formatErrorMessage(@NotNull String message) {
                return "Can't load configuration for the target host: " + message;
            }
        }

        record JumpHost(@NotNull String configurationPrefix, int index) implements ConfigurationKind {
            @NotNull
            @Override
            public String formatErrorMessage(@NotNull String message) {
                return "Can't load configuration for the jump host #" + (index + 1) + ": " + message;
            }
        }
    }
}
