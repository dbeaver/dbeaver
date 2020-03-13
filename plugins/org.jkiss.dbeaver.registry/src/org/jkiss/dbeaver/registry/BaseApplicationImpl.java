/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.DefaultSecureStorage;

/**
 * Base application implementation
 */
public abstract class BaseApplicationImpl implements IApplication, DBPApplication {

    private static final Log log = Log.getLog(BaseApplicationImpl.class);

    private static DBPApplication INSTANCE;

    protected BaseApplicationImpl() {
        if (INSTANCE != null && !(INSTANCE instanceof EclipseApplicationImpl)) {
            log.error("Multiple application instances created: " + INSTANCE.getClass().getName() + ", " + this.getClass().getName());
        }
        INSTANCE = this;
    }

    public static DBPApplication getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EclipseApplicationImpl();
        }
        return INSTANCE;
    }

    @Override
    public boolean isStandalone() {
        return true;
    }

    @Override
    public boolean isPrimaryInstance() {
        return true;
    }

    @Override
    public boolean isHeadlessMode() {
        return false;
    }

    @NotNull
    @Override
    public DBASecureStorage getSecureStorage() {
        return DefaultSecureStorage.INSTANCE;
    }

    @NotNull
    @Override
    public DBASecureStorage getProjectSecureStorage(DBPProject project) {
        return new ProjectSecureStorage(project);
    }

    @Override
    public String getInfoDetails() {
        return "N/A";
    }

    /////////////////////////////////////////
    // IApplication

    @Override
    public Object start(IApplicationContext context) throws Exception {
        return EXIT_OK;
    }

    @Override
    public void stop() {

    }

}
