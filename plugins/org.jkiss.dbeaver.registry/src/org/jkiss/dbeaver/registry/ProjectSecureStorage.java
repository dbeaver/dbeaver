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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPProject;

import javax.crypto.SecretKey;

public class ProjectSecureStorage implements DBASecureStorage {
    private static final Log log = Log.getLog(ProjectSecureStorage.class);

    protected final DBPProject project;
    protected final DBASecureStorage globalStorage;

    public ProjectSecureStorage(DBPProject project) {
        this.project = project;
        globalStorage = project.getWorkspace().getPlatform().getApplication().getSecureStorage();
    }

    public DBPProject getProject() {
        return project;
    }

    public DBASecureStorage getGlobalStorage() {
        return globalStorage;
    }

    @Override
    public boolean useSecurePreferences() {
        return globalStorage.useSecurePreferences();
    }

    @Override
    public ISecurePreferences getSecurePreferences() {
        return project.getWorkspace().getPlatform().getApplication().getSecureStorage().getSecurePreferences().node("projects").node(project.getName());
    }

    @Override
    public SecretKey getLocalSecretKey() {
        return globalStorage.getLocalSecretKey();
    }

}
