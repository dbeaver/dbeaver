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
package com.dbeaver.db.cdata.registry;

import com.dbeaver.db.cdata.CDataLicenseUIService;
import com.dbeaver.db.cdata.model.CDataIcons;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDriverLicense;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLazyIcon;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLicense;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLoaderDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CDataDriverDescriptor extends DriverDescriptor implements DBPDriverWithLicense, DBPDriverWithLazyIcon {
    private static final long ICON_RETRY_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final CDataDriverInfo driverInfo;
    private final Object licenseActivationLock = new Object();
    private final AtomicBoolean activationDialogInProgress = new AtomicBoolean();
    private final AtomicBoolean activationProcessInProgress = new AtomicBoolean();
    private final AtomicBoolean iconLoadStarted = new AtomicBoolean();
    private final Set<Runnable> iconUpdateCallbacks = ConcurrentHashMap.newKeySet();
    private volatile boolean iconLoaded;
    private volatile long iconRetryAfterNanos;
    private volatile DBPImage driverIcon = DBIcon.DATABASE_DEFAULT;
    private volatile DBPImage driverIconBig = DBIcon.DATABASE_BIG_DEFAULT;
    private volatile CDataDriverLicense currentLicense = new CDataDriverLicense(
        CDataLicenseStatus.VALIDATION_UNAVAILABLE,
        "",
        null
    );

    public CDataDriverDescriptor(
        @NotNull DataSourceProviderDescriptor providerDescriptor,
        @NotNull String id,
        @NotNull CDataDriverInfo driverInfo
    ) {
        super(providerDescriptor, id);
        this.driverInfo = driverInfo;
        setCustom(false);
        updateIcons();
    }

    @NotNull
    public CDataDriverInfo getDriverInfo() {
        return driverInfo;
    }

    @Override
    public void loadIcon(@NotNull Runnable onUpdate) {
        if (iconLoaded || System.nanoTime() < iconRetryAfterNanos) {
            return;
        }
        iconUpdateCallbacks.add(onUpdate);
        if (iconLoadStarted.compareAndSet(false, true)) {
            CDataDriverIconLoader.load(driverInfo.dataSource(), (icon, iconBig) -> {
                if (icon != null && iconBig != null) {
                    updateDriverIcons(icon, iconBig);
                    iconLoaded = true;
                    for (Runnable callback : iconUpdateCallbacks) {
                        callback.run();
                    }
                    iconUpdateCallbacks.clear();
                } else {
                    iconUpdateCallbacks.clear();
                    iconRetryAfterNanos = System.nanoTime() + ICON_RETRY_NANOS;
                    iconLoadStarted.set(false);
                }
            });
        }
    }

    @NotNull
    @Override
    public synchronized DriverLoaderDescriptor getDefaultDriverLoader() {
        return super.getDefaultDriverLoader();
    }

    @NotNull
    @Override
    protected DriverLoaderDescriptor createDriverLoader(@NotNull String loaderId) {
        return new CDataDriverLoaderDescriptor(loaderId, this);
    }

    @NotNull
    public CDataResolvedDriver resolveDriver(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getCDataDriverLoader().resolveDriver(monitor);
    }

    @NotNull
    public CDataDriverLicense validateCurrentLicense(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getCDataDriverLoader().validateLicense(monitor, true);
    }

    public boolean beginLicenseActivationDialog() {
        return activationDialogInProgress.compareAndSet(false, true);
    }

    public void endLicenseActivationDialog() {
        activationDialogInProgress.set(false);
    }

    public boolean beginLicenseActivationProcess() {
        return activationProcessInProgress.compareAndSet(false, true);
    }

    public void endLicenseActivationProcess() {
        activationProcessInProgress.set(false);
    }

    @NotNull
    Object getLicenseActivationLock() {
        return licenseActivationLock;
    }

    public void invalidateLicenseCaches() {
        for (var loader : getAllDriverLoaders()) {
            ((CDataDriverLoaderDescriptor) loader).invalidateLicenseCache();
        }
    }

    @NotNull
    @Override
    public String getVendorName() {
        return "CData";
    }

    @NotNull
    @Override
    public String getVendorURL() {
        return "https://cdata.com";
    }

    @NotNull
    @Override
    public String getDriverPurchaseURL() {
        return driverInfo.purchaseUrl();
    }

    @NotNull
    @Override
    public DBPDriverLicense getCurrentLicense() throws DBException {
        return currentLicense;
    }

    @NotNull
    public CDataLicenseStatus getLicenseStatus() {
        return currentLicense.getStatus();
    }

    void setCurrentLicense(@NotNull CDataDriverLicense currentLicense) {
        this.currentLicense = currentLicense;
        updateIcons();
    }

    @Override
    public boolean supportsTrialLicense() {
        return true;
    }

    @NotNull
    @Override
    public DBPDriverLicense requestTrialLicense(@NotNull DBRProgressMonitor monitor) throws DBException {
        CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
        if (uiService == null) {
            throw new DBException("CDATA license activation UI is unavailable");
        }
        CDataDriverLicense license = uiService.activateLicense(this, CDataLicenseType.TRIAL);
        if (license == null) {
            throw new DBException("CDATA trial license activation was canceled");
        }
        return license;
    }

    @NotNull
    private CDataDriverLoaderDescriptor getCDataDriverLoader() {
        return (CDataDriverLoaderDescriptor) getDefaultDriverLoader();
    }

    private synchronized void updateIcons() {
//        DBPImage statusOverlay = switch (currentLicense.getStatus()) {
//            case TRIAL_ACTIVE, TRIAL_EXPIRING -> DBIcon.OVER_LAMP;
//            case PURCHASED_ACTIVE, PURCHASED_EXPIRING -> DBIcon.OVER_SUCCESS;
//            case TRIAL_EXPIRED, EXPIRED, INVALID_KEY, MACHINE_MISMATCH, WRONG_MAJOR_VERSION -> DBIcon.OVER_ERROR;
//            case VALIDATION_UNAVAILABLE -> DBIcon.OVER_UNKNOWN;
//            case NOT_INSTALLED -> null;
//        };
        setIconPlain(new DBIconComposite(
            driverIcon,
            false,
            null,
            null,
            null,
            CDataIcons.CDATA_OVERLAY
        ));
        setIconBig(new DBIconComposite(
            driverIconBig,
            false,
            null,
            null,
            null,
            CDataIcons.CDATA_OVERLAY_BIG
        ));
    }

    private synchronized void updateDriverIcons(@NotNull DBPImage icon, @NotNull DBPImage iconBig) {
        driverIcon = icon;
        driverIconBig = iconBig;
        updateIcons();
    }
}
