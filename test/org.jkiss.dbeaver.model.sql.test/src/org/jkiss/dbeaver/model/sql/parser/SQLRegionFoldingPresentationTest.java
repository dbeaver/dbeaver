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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SQLRegionFoldingPresentationTest extends DBeaverUnitTest {

    private static final List<SQLRegionMarkerFolding.RegionFold> SAMPLE_REGIONS = List.of(
        new SQLRegionMarkerFolding.RegionFold("OUTER", 0, 120)
    );

    @Test
    public void editBeforeRegionRequiresGutterRefresh() {
        int editOffset = 0;
        Assertions.assertTrue(SQLRegionMarkerFolding.documentEditShiftsRegionMarkers(editOffset, SAMPLE_REGIONS));
        Assertions.assertTrue(SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            false,
            false,
            false,
            editOffset,
            SAMPLE_REGIONS
        ));
    }

    @Test
    public void editOnRegionMarkerRequiresGutterRefresh() {
        int editOffset = SAMPLE_REGIONS.getFirst().offset();
        Assertions.assertTrue(SQLRegionMarkerFolding.documentEditShiftsRegionMarkers(editOffset, SAMPLE_REGIONS));
        Assertions.assertTrue(SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            false,
            false,
            false,
            editOffset,
            SAMPLE_REGIONS
        ));
    }

    @Test
    public void editAfterAllRegionsDoesNotRequireGutterRefresh() {
        int editOffset = SAMPLE_REGIONS.getFirst().offset() + SAMPLE_REGIONS.getFirst().length() + 10;
        Assertions.assertFalse(SQLRegionMarkerFolding.documentEditShiftsRegionMarkers(editOffset, SAMPLE_REGIONS));
        Assertions.assertFalse(SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            false,
            false,
            false,
            editOffset,
            SAMPLE_REGIONS
        ));
    }

    @Test
    public void widenedDamagedRegionOffsetMustNotTriggerRefreshForEditsInsideRegion() {
        int originalEditOffset = 80;
        int widenedDamagedRegionOffset = SAMPLE_REGIONS.getFirst().offset();

        Assertions.assertFalse(SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            false,
            false,
            false,
            originalEditOffset,
            SAMPLE_REGIONS
        ));
        Assertions.assertTrue(SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            false,
            false,
            false,
            widenedDamagedRegionOffset,
            SAMPLE_REGIONS
        ));
    }
}
