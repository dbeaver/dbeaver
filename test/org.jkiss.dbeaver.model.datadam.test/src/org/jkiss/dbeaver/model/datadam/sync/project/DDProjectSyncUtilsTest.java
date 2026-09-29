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
package org.jkiss.dbeaver.model.datadam.sync.project;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DDProjectSyncUtilsTest {

    @Test
    void unchangedWhenNothingChanged() {
        assertChange(DDSyncChange.UNCHANGED, "original", "original", "original");
    }

    @Test
    void localWhenOnlyLocalChanged() {
        assertChange(DDSyncChange.LOCAL, "original", "local change", "original");
    }

    @Test
    void serverWhenOnlyServerChanged() {
        assertChange(DDSyncChange.SERVER, "original", "original", "server change");
    }

    @Test
    void conflictWhenLocalAndServerChangedDifferently() {
        assertChange(DDSyncChange.CONFLICT, "original", "local change", "server change");
    }

    @Test
    void serverWhenLocalAndServerChangedIdentically() {
        assertChange(DDSyncChange.SERVER, "original", "same change", "same change");
    }

    private static void assertChange(
        @NotNull DDSyncChange expectedChange,
        @NotNull String baselineFingerprint,
        @NotNull String localFingerprint,
        @NotNull String serverFingerprint
    ) {
        assertEquals(
            expectedChange,
            DDProjectSyncUtils.classify(localFingerprint, baselineFingerprint, serverFingerprint)
        );
    }
}
