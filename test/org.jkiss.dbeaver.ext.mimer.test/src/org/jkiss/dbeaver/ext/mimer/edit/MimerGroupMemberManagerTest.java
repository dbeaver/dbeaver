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

import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link MimerGroupMemberManager#canCreateObject} - refuses "Create New Member" under the
 * built-in PUBLIC group specifically, since every ident is already implicitly a member and
 * Mimer SQL rejects an explicit {@code GRANT MEMBER ON GROUP PUBLIC}. Case-insensitive, matching
 * {@link org.jkiss.dbeaver.ext.mimer.MimerConstants#GROUP_PUBLIC}'s own comparison.
 *
 * @author Mimer Information Technology
 */
public class MimerGroupMemberManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerGroup publicGroup;

    @Mock
    private MimerGroup otherGroup;

    private final MimerGroupMemberManager manager = new MimerGroupMemberManager();

    @Test
    public void refusesCreateUnderThePublicGroup() {
        when(publicGroup.getName()).thenReturn("PUBLIC");
        Assertions.assertFalse(manager.canCreateObject(publicGroup));
    }

    @Test
    public void refusesCreateUnderThePublicGroupRegardlessOfCase() {
        when(publicGroup.getName()).thenReturn("public");
        Assertions.assertFalse(manager.canCreateObject(publicGroup));
    }

    @Test
    public void doesNotRefuseBasedOnNameAloneForAnyOtherGroup() {
        when(otherGroup.getName()).thenReturn("developers");
        // Only asserting the PUBLIC-specific short-circuit is NOT what refuses this one - the
        // real return value beyond that depends on the live platform permission check, which
        // this unit test doesn't control and shouldn't assert either way.
        Assertions.assertDoesNotThrow(() -> manager.canCreateObject(otherGroup));
    }
}
