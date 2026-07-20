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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.dbeaver.ext.generic.model.ManticoreSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Manticore does not support table aliases; generated SELECT/View Data must not use them.
 */
public class ManticoreSQLDialectTest extends DBeaverUnitTest {

    private ManticoreSQLDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new ManticoreSQLDialect();
    }

    @Test
    public void supportsAliasInSelectShouldBeFalse() {
        Assertions.assertFalse(dialect.supportsAliasInSelect(),
            "Manticore Search does not support table aliases in SELECT/FROM");
    }

    @Test
    public void supportsAliasInUpdateShouldBeFalse() {
        Assertions.assertFalse(dialect.supportsAliasInUpdate());
    }

    @Test
    public void supportsAliasInConditionsShouldBeFalse() {
        Assertions.assertFalse(dialect.supportsAliasInConditions());
    }

    @Test
    public void dialectNameShouldIdentifyManticore() {
        Assertions.assertEquals("Manticore Search", dialect.getDialectName());
    }
}
