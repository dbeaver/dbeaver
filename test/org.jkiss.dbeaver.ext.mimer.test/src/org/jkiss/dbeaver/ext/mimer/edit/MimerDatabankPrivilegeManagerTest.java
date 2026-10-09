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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * {@link MimerDatabankPrivilegeManager#canCreateObject} - Mimer SQL's built-in databanks can't
 * be granted on, so the "Privileges" folder is hidden for them (plugin.xml {@code visibleIf}) and
 * this backstop refuses "Create New Privilege" there too.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankPrivilegeManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    private final MimerDatabankPrivilegeManager manager = new MimerDatabankPrivilegeManager();

    @Test
    public void refusesToGrantOnABuiltInSystemDatabank() {
        for (String name : new String[]{"SQLDB", "TRANSDB", "LOGDB", "SYSDB"}) {
            Assertions.assertFalse(
                manager.canCreateObject(new MimerDatabank(dataSource, name)),
                () -> name + " privileges must not be creatable");
        }
    }

    @Test
    public void allowsGrantingOnAnOrdinaryDatabank() {
        // Falls through to the same live permission check as everywhere else - just check the
        // system-databank short-circuit is ours and doesn't blow up.
        Assertions.assertDoesNotThrow(() -> manager.canCreateObject(new MimerDatabank(dataSource, "MYDB")));
    }
}
