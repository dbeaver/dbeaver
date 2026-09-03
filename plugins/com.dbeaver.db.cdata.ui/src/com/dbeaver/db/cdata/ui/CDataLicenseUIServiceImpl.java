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
package com.dbeaver.db.cdata.ui;

import com.dbeaver.db.cdata.CDataLicenseUIService;
import com.dbeaver.db.cdata.registry.CDataDriverDescriptor;
import com.dbeaver.db.cdata.registry.CDataDriverLicense;
import com.dbeaver.db.cdata.registry.CDataLicenseType;
import org.eclipse.jface.window.Window;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class CDataLicenseUIServiceImpl implements CDataLicenseUIService {
    @Nullable
    @Override
    public CDataDriverLicense activateLicense(
        @NotNull CDataDriverDescriptor driver,
        @Nullable CDataLicenseType fixedType
    ) {
        if (!driver.beginLicenseActivationDialog()) {
            return null;
        }
        try {
            return new UITask<CDataDriverLicense>() {
                @Override
                protected CDataDriverLicense runTask() {
                    CDataActivationDialog dialog = new CDataActivationDialog(UIUtils.getActiveShell(), driver, fixedType);
                    return dialog.open() == Window.OK ? dialog.getActivatedLicense() : null;
                }
            }.execute();
        } finally {
            driver.endLicenseActivationDialog();
        }
    }
}
