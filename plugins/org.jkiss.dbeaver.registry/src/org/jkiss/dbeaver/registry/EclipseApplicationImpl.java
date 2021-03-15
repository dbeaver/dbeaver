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
package org.jkiss.dbeaver.registry;

import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.DefaultSecureStorage;

/**
 * EclipseApplicationImpl
 */
class EclipseApplicationImpl extends BaseApplicationImpl {

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean isPrimaryInstance() {
        return false;
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
        return "Eclipse";
    }

    @Override
    public String getDefaultProjectName() {
        return "DBeaver";
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        return null;
    }

    @Override
    public void stop() {

    }
}
