/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.jface.window.Window;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverLicense;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseType;
import org.jkiss.dbeaver.ext.cdata.registry.CDataResolvedDriver;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;
import java.util.WeakHashMap;

public class CDataLicenseUIServiceImpl implements CDataLicenseUIService {
    private final Map<CDataDriverDescriptor, ActivationIdentity> trialIdentities = new WeakHashMap<>();

    @Nullable
    @Override
    public CDataDriverLicense activateLicense(
        @NotNull CDataDriverDescriptor driver,
        @Nullable CDataLicenseType fixedType
    ) {
        return activateLicense(driver, fixedType, null);
    }

    @Nullable
    @Override
    public CDataDriverLicense activateLicense(
        @NotNull CDataDriverDescriptor driver,
        @Nullable CDataLicenseType fixedType,
        @Nullable CDataResolvedDriver activationTarget
    ) {
        if (!driver.beginLicenseActivationDialog()) {
            return null;
        }
        try {
            return new UITask<CDataDriverLicense>() {
                @Override
                protected CDataDriverLicense runTask() {
                    ActivationIdentity identity = trialIdentities.getOrDefault(driver, ActivationIdentity.EMPTY);
                    CDataActivationDialog dialog = new CDataActivationDialog(
                        UIUtils.getActiveShell(),
                        driver,
                        fixedType,
                        activationTarget,
                        identity.name(),
                        identity.email()
                    );
                    if (dialog.open() != Window.OK) {
                        return null;
                    }
                    CDataDriverLicense license = dialog.getActivatedLicense();
                    if (license != null && dialog.getSelectedType() == CDataLicenseType.TRIAL) {
                        trialIdentities.put(driver, new ActivationIdentity(
                            dialog.getEndUserName(),
                            dialog.getEndUserEmail()
                        ));
                    } else {
                        trialIdentities.remove(driver);
                    }
                    return license;
                }
            }.execute();
        } finally {
            driver.endLicenseActivationDialog();
        }
    }

    private record ActivationIdentity(@NotNull String name, @NotNull String email) {
        private static final ActivationIdentity EMPTY = new ActivationIdentity("", "");
    }
}
