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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class DBVModelCacheTest extends DBeaverUnitTest {

    @Test
    void resetRemovesForeignKeysFromGlobalCache() {
        String refId = "dbv-cache-test-reset";
        DBVModel model = createModel(refId);
        try {
            Assertions.assertEquals(1, DBVModel.getGlobalReferenceCache().get(refId).size());

            model.resetData();

            Assertions.assertFalse(DBVModel.getGlobalReferenceCache().containsKey(refId));
        } finally {
            model.dispose();
        }
    }

    @Test
    void copyingModelRemovesPreviousForeignKeysFromGlobalCache() {
        String oldRefId = "dbv-cache-test-old";
        String newRefId = "dbv-cache-test-new";
        DBVModel model = createModel(oldRefId);
        DBVModel replacement = createModel(newRefId);
        try {
            model.copyFrom(replacement);

            Assertions.assertFalse(DBVModel.getGlobalReferenceCache().containsKey(oldRefId));
            Assertions.assertEquals(2, DBVModel.getGlobalReferenceCache().get(newRefId).size());
        } finally {
            model.dispose();
            replacement.dispose();
        }
        Assertions.assertFalse(DBVModel.getGlobalReferenceCache().containsKey(newRefId));
    }

    private static DBVModel createModel(String refId) {
        return new DBVModel("test", Map.of(
            "schema", Map.of(
                ":source", Map.of(
                    "foreign-keys", List.of(Map.of("entity", refId, "constraint", "PRIMARY"))
                )
            )
        ));
    }
}
