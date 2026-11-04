/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.update;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.services.UIServiceApplicationVersionUpdater;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Version checker job
 */
public class DBeaverVersionChecker extends AbstractJob {

    private static final Log log = Log.getLog(DBeaverVersionChecker.class);
    private static final String PROP_OVERRIDE_PRODUCT_VERSION = "dbeaver.debug.override-product-version";
    private static final String PROP_OVERRIDE_PRODUCT_PATCH = "dbeaver.debug.override-product-patch";
    private static final String PROP_SKIP_VERSION_CHECK = "dbeaver.debug.skip-version-check";
    private static final String PROP_VERSION_DESCRIPTOR = "dbeaver.debug.version-descriptor";
    private static final String PROP_VERSION_UPDATE_URL = "versionUpdateURL";
    private static final String PREF_SUPPRESS_UPDATE_CHECK_PREFIX = "suppressUpdateCheck.";

    private static final boolean SKIP_VERSION_CHECK;
    private static final Version OVERRIDE_PRODUCT_VERSION;

    static {
        String versionProperty = System.getProperty(PROP_OVERRIDE_PRODUCT_VERSION);
        Version version = null;

        if (CommonUtils.isNotEmpty(versionProperty)) {
            try {
                version = new Version(versionProperty);
            } catch (Exception e) {
                log.debug("Cannot parse override version '" + versionProperty + "'", e);
            }
        }

        OVERRIDE_PRODUCT_VERSION = version;
        SKIP_VERSION_CHECK = CommonUtils.toBoolean(System.getProperty(PROP_SKIP_VERSION_CHECK));
    }

    private final boolean showAlways;

    public DBeaverVersionChecker(boolean force)
    {
        super("DBeaver new version release checker");
        this.showAlways = force;
        setUser(false);
        setSystem(true);
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor)
    {
        if (monitor.isCanceled() || DBWorkbench.getPlatform().isShuttingDown()) {
            return Status.CANCEL_STATUS;
        }
        boolean showUpdateDialog = showAlways;
        if (!showUpdateDialog) {
            // Check for auto-update settings
            showUpdateDialog = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK);
        }
        if (!showAlways && !showUpdateDialog) {
            return Status.OK_STATUS;
        }

        IProduct product = Platform.getProduct();
        if (product == null) {
            // No product!
            log.error("No Eclipse product found. Installation is corrupted");
            return Status.OK_STATUS;
        }
        final String updateURL = product.getProperty(PROP_VERSION_UPDATE_URL);
        if (updateURL == null) {
            return Status.OK_STATUS;
        }

        final Version currentVersion = getProductVersion();
        final VersionDescriptor newVersion;

        try {
            String descriptorFile = System.getProperty(PROP_VERSION_DESCRIPTOR);
            if (CommonUtils.isNotEmpty(descriptorFile)) {
                try (var input = Files.newInputStream(Path.of(descriptorFile))) {
                    newVersion = new VersionDescriptor(input);
                }
            } else {
                newVersion = new VersionDescriptor(DBWorkbench.getPlatform(), updateURL);
            }
        } catch (IOException | InvalidPathException e) {
            if (showAlways) {
                // Show error dialog only if fired by user
                DBWorkbench.getPlatformUI().showError(CoreMessages.dialog_version_update_title, CoreMessages.dialog_version_update_error_cannot_check_version, e);
            }
            return Status.CANCEL_STATUS;
        }

        boolean newVersionAvailable = newVersion.isNewerThan(currentVersion, getProductPatchNumber());
        boolean suppressed = isSuppressed(newVersion);
        UIServiceApplicationVersionUpdater updater = DBWorkbench.findService(UIServiceApplicationVersionUpdater.class);
        boolean showToolbarNotification = updater == null && newVersionAvailable && !showAlways;
        if (showAlways || (!suppressed && (SKIP_VERSION_CHECK || newVersionAvailable))) {
            if (updater != null) {
                UIUtils.asyncExec(updater::handleVersionUpdate);
            } else if (showToolbarNotification) {
                UIUtils.asyncExec(() -> VersionUpdateHandler.showNotification(currentVersion, newVersion));
            } else {
                showUpdaterDialog(currentVersion, newVersion);
            }
        }

        return Status.OK_STATUS;
    }

    private void showUpdaterDialog(@NotNull Version currentVersion, @NotNull VersionDescriptor newVersion)
    {
        UIUtils.asyncExec(() -> {
            DBeaverApplication.getInstance().notifyVersionUpgrade(currentVersion, newVersion, !showAlways);
        });
    }

    static boolean isSuppressed(@NotNull VersionDescriptor version) {
        return CoreApplicationActivator.getDefault().getPreferenceStore().getBoolean(getSuppressionKey(version));
    }

    @NotNull
    static String getSuppressionKey(@NotNull VersionDescriptor version) {
        return PREF_SUPPRESS_UPDATE_CHECK_PREFIX + version.getUpdateVersion();
    }

    static int getProductPatchNumber() {
        if (OVERRIDE_PRODUCT_VERSION != null) {
            // The override is an OSGi version; its qualifier is not a numeric product patch.
            return Math.max(0, Integer.getInteger(PROP_OVERRIDE_PRODUCT_PATCH, 0));
        }
        return GeneralUtils.getProductPatchNumber();
    }

    @NotNull
    private static Version getProductVersion() {
        return OVERRIDE_PRODUCT_VERSION == null ? GeneralUtils.getProductVersion() : OVERRIDE_PRODUCT_VERSION;
    }
}
