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
package org.jkiss.dbeaver.registry.rm;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceFolder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.AdditionalMatchers.aryEq;

public class DataSourceRegistryRMTest {
    @Test
    public void testNestedFolderDeletionIsPersistedOnce() throws DBException {
        DBPProject project = Mockito.mock(DBPProject.class);
        RMController rmController = Mockito.mock(RMController.class);
        DBPPreferenceStore preferenceStore = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(project.getId()).thenReturn("project");

        DataSourceRegistryRM<DataSourceDescriptor> registry =
            new DataSourceRegistryRM<>(project, rmController, preferenceStore);
        DataSourceFolder root = registry.addFolder(null, "root");
        DataSourceFolder child = registry.addFolder(root, "child");
        registry.addFolder(child, "grandchild");
        Mockito.reset(rmController);

        registry.removeFolder(root, false);

        Mockito.verify(rmController).deleteProjectDataSourceFolders(
            Mockito.eq("project"), aryEq(new String[]{"root"}), Mockito.eq(false));
        Mockito.verifyNoMoreInteractions(rmController);
    }
}
