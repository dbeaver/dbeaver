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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OracleSnapshotChangesTest extends DBeaverUnitTest {

    @Test
    public void nothingChanged() {
        Map<String, List<String>> rows = Map.of(
            "T1", List.of("TABLE|1000|VALID"),
            "MV1", List.of("TABLE|2000|VALID", "MATERIALIZED VIEW|2000|VALID"));
        Assertions.assertEquals(Set.of(), OracleSchema.findChangedObjects(rows, rows));
    }

    @Test
    public void findsChangedNewAndDroppedObjects() {
        Map<String, List<String>> stored = Map.of(
            "T1", List.of("TABLE|1000|VALID"),
            "T2", List.of("TABLE|1000|VALID"),
            "V1", List.of("VIEW|1000|VALID"),
            "MV1", List.of("TABLE|2000|VALID", "MATERIALIZED VIEW|2000|VALID"));
        Map<String, List<String>> current = Map.of(
            // DDL on T1
            "T1", List.of("TABLE|5000|VALID"),
            // V1 invalidated by a change of an object it depends on: same DDL time
            "V1", List.of("VIEW|1000|INVALID"),
            // Same rows in another order
            "MV1", List.of("MATERIALIZED VIEW|2000|VALID", "TABLE|2000|VALID"),
            // Created
            "T3", List.of("TABLE|6000|VALID"));
        // T2 dropped
        Assertions.assertEquals(Set.of("T1", "V1", "T2", "T3"), OracleSchema.findChangedObjects(stored, current));
    }
}
