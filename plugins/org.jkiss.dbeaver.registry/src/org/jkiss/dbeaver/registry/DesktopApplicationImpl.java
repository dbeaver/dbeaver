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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesMapper;
import org.eclipse.equinox.internal.security.storage.StorageUtils;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * EclipseApplicationImpl
 */
public abstract class DesktopApplicationImpl extends BaseApplicationImpl {
    private static final Log log = Log.getLog(DesktopApplicationImpl.class);

    public static final String DBEAVER_DATA_DIR = "DBeaverData"; //$NON-NLS-1$

    private static final String DBEAVER_SECURE_DIR = "secure"; //$NON-NLS-1$
    private static final String DBEAVER_SECURE_FILE = "secure_storage"; //$NON-NLS-1$
    private static final String ECLIPSE_KEYRING = "-eclipse.keyring"; //$NON-NLS-1$
    private static final String ECLIPSE_APP_ARGUMENTS_FIELD = "appArgs"; //$NON-NLS-1$
    
    private static final Path STORAGE_PATH =
        Path.of(RuntimeUtils.getWorkingDirectory(DBEAVER_DATA_DIR)).resolve(DBEAVER_SECURE_DIR)
        .resolve(DBEAVER_SECURE_FILE);

    @Override
    public Object start(IApplicationContext context)  {
        if (isStandalone() && !isDistributed()) {
            updateSecretStorage();
        }
        return null;
    }

    private void updateSecretStorage() {
        try {
            tryCreateSecretFile();
        } catch (DBException e) {
            log.error(e);
        }
        EnvironmentInfo environmentInfoService = AuthPlugin.getDefault().getEnvironmentInfoService();
        if (environmentInfoService instanceof EquinoxConfiguration) {
            String[] nonFrameworkArgs = environmentInfoService.getNonFrameworkArgs();
            if (!isDistributed() && Files.exists(STORAGE_PATH) && Arrays.stream(nonFrameworkArgs)
                .filter(it -> it.equals(ECLIPSE_KEYRING)).findAny().isEmpty()) {
                // Unfortunately the Equinox reads the eclipse.keyring from arguments
                // before any DBeaver controlled part is executed and there is no way
                // to modify the variable after that without reflection.
                String[] updatedArgs = Arrays.copyOf(nonFrameworkArgs, nonFrameworkArgs.length + 2);
                updatedArgs[updatedArgs.length - 2] = ECLIPSE_KEYRING;
                updatedArgs[updatedArgs.length - 1] = STORAGE_PATH.toString();
                ((EquinoxConfiguration) environmentInfoService).setAppArgs(updatedArgs);
                SecurePreferencesMapper.clearDefault();
            }
        }
    }

    private void migrateFromEclipseStorage() throws IOException, URISyntaxException {
        Path oldLocation = Path.of(StorageUtils.getDefaultLocation().toURI());
        log.debug("Migrating to " + STORAGE_PATH.toString() + " from " + StorageUtils.getDefaultLocation());
        Files.createDirectories(STORAGE_PATH.getParent());
        Files.copy(oldLocation, STORAGE_PATH, StandardCopyOption.REPLACE_EXISTING);
    }

    private void tryCreateSecretFile() throws DBException {
        try {
            if (!Files.exists(STORAGE_PATH)) {
                if (Files.exists(Path.of(StorageUtils.getDefaultLocation().toURI()))) {
                    migrateFromEclipseStorage();
                } else {
                    Files.createDirectories(STORAGE_PATH.getParent());
                    Files.createFile(STORAGE_PATH);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new DBException("Error migrating secure storage file", e);
        }
    }


    @NotNull
    @Override
    public DBPWorkspaceDesktop createWorkspace(@NotNull DBPPlatform platform, @NotNull IWorkspace eclipseWorkspace) {
        return new DesktopWorkspaceImpl(platform, eclipseWorkspace);
    }

}
