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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FireBirdFieldTypeTest extends DBeaverUnitTest {

    private static final int SMALLINT_ID = 7;
    private static final int INTEGER_ID = 8;
    private static final int BIGINT_ID = 16;
    private static final int INT128_ID = 26;

    @Test
    public void resolvesExactNumericsForEveryStorageType() {
        for (int id : new int[]{SMALLINT_ID, INTEGER_ID, BIGINT_ID, INT128_ID}) {
            Assertions.assertEquals(FireBirdFieldType.NUMERIC, FireBirdFieldType.getById(id, 1), "type " + id);
            Assertions.assertEquals(FireBirdFieldType.DECIMAL, FireBirdFieldType.getById(id, 2), "type " + id);
        }
    }

    @Test
    public void resolvesIntegersWithoutSubtype() {
        Assertions.assertEquals(FireBirdFieldType.SMALLINT, FireBirdFieldType.getById(SMALLINT_ID, 0));
        Assertions.assertEquals(FireBirdFieldType.INTEGER, FireBirdFieldType.getById(INTEGER_ID, 0));
        Assertions.assertEquals(FireBirdFieldType.BIGINT, FireBirdFieldType.getById(BIGINT_ID, 0));
        Assertions.assertEquals(FireBirdFieldType.INT128, FireBirdFieldType.getById(INT128_ID, 0));
    }

    @Test
    public void ignoresSubtypeOfNonIntegerTypes() {
        // Subtype of CHAR/VARCHAR is the character set related subtype, BLOB subtype 1 is TEXT
        Assertions.assertEquals(FireBirdFieldType.VARCHAR, FireBirdFieldType.getById(37, 1));
        Assertions.assertEquals(FireBirdFieldType.BLOB, FireBirdFieldType.getById(261, 1));
    }

    @Test
    public void returnsNullForUnknownType() {
        Assertions.assertNull(FireBirdFieldType.getById(999, 0));
    }
}
