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
package org.jkiss.dbeaver.headless;

import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.registry.BaseApplicationImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Headless application
 */
public class DBeaverHeadlessApplication extends BaseApplicationImpl {

    private static final Log log = Log.getLog(DBeaverHeadlessApplication.class);

    @Override
    public Object start(IApplicationContext context) {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        System.out.println("Starting headless test application " + application.getClass().getName());

        return null;
    }

    @Override
    public void stop() {
        System.out.println("Starting headless test application");
        super.stop();
    }

    @Override
    public String getDefaultProjectName() {
        return "DBeaverTests";
    }

}
