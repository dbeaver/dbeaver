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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class FireBirdTriggerTypeTest extends DBeaverUnitTest {

    private static final String[] PHASES = {"BEFORE", "AFTER"};
    private static final String[] ACTIONS = {null, "INSERT", "UPDATE", "DELETE"};

    @Test
    public void resolvesUpdateOrInsertTrigger() {
        FireBirdTriggerType type = FireBirdTriggerType.getByType(12);
        Assertions.assertEquals(FireBirdTriggerType.AFTER_UPDATE_OR_INSERT, type);
        Assertions.assertEquals("AFTER UPDATE OR INSERT", type.getDisplayName());
    }

    @Test
    public void resolvesEveryDmlActionOrder() {
        List<int[]> actionOrders = new ArrayList<>();
        for (int first = 1; first <= 3; first++) {
            actionOrders.add(new int[]{first});
            for (int second = 1; second <= 3; second++) {
                if (second == first) {
                    continue;
                }
                actionOrders.add(new int[]{first, second});
                actionOrders.add(new int[]{first, second, 6 - first - second});
            }
        }
        for (int phase = 0; phase < PHASES.length; phase++) {
            for (int[] actions : actionOrders) {
                // Same encoding as Firebird isql (src/isql/show.epp): bit 0 of (type + 1) is the phase,
                // then each action takes two bits starting from bit 1
                int code = phase;
                StringBuilder expectedName = new StringBuilder(PHASES[phase]);
                for (int slot = 0; slot < actions.length; slot++) {
                    code |= actions[slot] << (slot * 2 + 1);
                    expectedName.append(slot == 0 ? " " : " OR ").append(ACTIONS[actions[slot]]);
                }
                FireBirdTriggerType type = FireBirdTriggerType.getByType(code - 1);
                Assertions.assertNotNull(type, "Unresolved trigger type " + (code - 1) + " (" + expectedName + ")");
                Assertions.assertEquals(expectedName.toString(), type.getDisplayName());
                Assertions.assertFalse(type.isDbEvent());
            }
        }
    }

    @Test
    public void showsSourceOfTriggerWithUnknownType() throws Exception {
        GenericStructContainer container = Mockito.mock(GenericStructContainer.class);
        FireBirdDatabaseTrigger trigger = new FireBirdDatabaseTrigger(container, "T_UNKNOWN", null, null, 0, false);

        Assertions.assertNull(trigger.getTriggerType());
        Assertions.assertEquals(
            "CREATE TRIGGER T_UNKNOWN\nAS\nBEGIN\nEND",
            FireBirdUtils.getTriggerSourceWithHeader(Mockito.mock(DBRProgressMonitor.class), trigger, "AS\nBEGIN\nEND")
        );
    }
}
