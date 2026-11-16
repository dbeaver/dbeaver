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
package org.jkiss.dbeaver.model.task;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DBTaskUtilsTest extends DBeaverUnitTest {
    @Test
    void usesLocalizedTaskTypeName() {
        DBTTaskManager manager = Mockito.mock(DBTTaskManager.class);
        DBTTaskType type = Mockito.mock(DBTTaskType.class);
        Mockito.when(type.getName()).thenReturn("Импорт данных");

        assertEquals("Импорт данных", DBTaskUtils.generateTaskName(manager, type));
    }

    @Test
    void usesFirstAvailableNumericSuffix() {
        DBTTaskManager manager = Mockito.mock(DBTTaskManager.class);
        DBTTaskType type = Mockito.mock(DBTTaskType.class);
        DBTTask existingTask = Mockito.mock(DBTTask.class);
        Mockito.when(type.getName()).thenReturn("Импорт данных");
        Mockito.when(manager.getTaskByName("Импорт данных")).thenReturn(existingTask);
        Mockito.when(manager.getTaskByName("Импорт данных (1)")).thenReturn(existingTask);
        Mockito.when(manager.getTaskByName("Импорт данных (3)")).thenReturn(existingTask);

        assertEquals("Импорт данных (2)", DBTaskUtils.generateTaskName(manager, type));
    }
}
