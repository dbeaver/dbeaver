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
package org.jkiss.dbeaver.headless;

import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.registry.DesktopApplicationImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.nio.file.Path;

/**
 * Headless application
 */
public class DBeaverHeadlessApplication extends DesktopApplicationImpl {

    private static final Log log = Log.getLog(DBeaverHeadlessApplication.class);

    @Override
    public Object start(IApplicationContext context) {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (RuntimeUtils.isWindows() && ModelPreferences.getPreferences().getBoolean(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE)) {
            System.setProperty(GeneralUtils.PROP_TRUST_STORE_TYPE, GeneralUtils.VALUE_TRUST_STORE_TYPE_WINDOWS);
        }
        System.out.println("Starting headless test application " + application.getClass().getName());

        return null;
    }

    @Override
    public void stop() {
        System.out.println("Starting headless test application");
        super.stop();
    }

    @Override
    public @Nullable Path getDefaultWorkingFolder() {
        return null;
    }

    @Override
    public String getDefaultProjectName() {
        return "DBeaverTests";
    }

}
