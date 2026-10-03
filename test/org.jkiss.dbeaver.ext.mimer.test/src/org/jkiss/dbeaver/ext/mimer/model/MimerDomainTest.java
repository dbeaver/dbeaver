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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link MimerDomain#buildCreateDDL()} - {@code CREATE DOMAIN ... AS <type> [COLLATE ...]
 * [DEFAULT ...] [CONSTRAINT "name"] [CHECK(...)]}. The constraint name is only ever meaningful
 * alongside a CHECK clause - see the test for that specifically.
 *
 * @author Mimer Information Technology
 */
public class MimerDomainTest extends DBeaverUnitTest {

    @Mock
    private MimerSchema schema;

    @BeforeEach
    public void setUp() {
        when(schema.getName()).thenReturn("mimer_store");
    }

    @Test
    public void createDDLWithJustADataType() {
        MimerDomain domain = new MimerDomain(schema, "positive_int");
        domain.setDataType("INTEGER");

        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"positive_int\" AS\nINTEGER",
            domain.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesCollateWhenSet() {
        MimerDomain domain = new MimerDomain(schema, "name_type");
        domain.setDataType("CHARACTER VARYING(50)");
        domain.setCollation("\"INFORMATION_SCHEMA\".\"ISO8BIT\"");

        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"name_type\" AS\nCHARACTER VARYING(50) COLLATE \"INFORMATION_SCHEMA\".\"ISO8BIT\"",
            domain.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesDefaultWhenSet() {
        MimerDomain domain = new MimerDomain(schema, "flag");
        domain.setDataType("BOOLEAN");
        domain.setDefaultValue("FALSE");

        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"flag\" AS\nBOOLEAN\nDEFAULT FALSE",
            domain.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesCheckClauseWithoutAConstraintNameWhenNoneGiven() {
        MimerDomain domain = new MimerDomain(schema, "positive_int");
        domain.setDataType("INTEGER");
        domain.setCheckClause("VALUE > 0");

        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"positive_int\" AS\nINTEGER\nCHECK(VALUE > 0)",
            domain.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesTheConstraintNameOnlyAlongsideACheckClause() {
        MimerDomain domain = new MimerDomain(schema, "positive_int");
        domain.setDataType("INTEGER");
        domain.setConstraintName("positive_int_check"); // set, but no check clause below - must not appear
        String ddl = domain.buildCreateDDL();

        Assertions.assertFalse(ddl.contains("CONSTRAINT"), () -> "no CHECK clause, CONSTRAINT shouldn't appear: " + ddl);

        domain.setCheckClause("VALUE > 0");
        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"positive_int\" AS\nINTEGER\nCONSTRAINT \"positive_int_check\" CHECK(VALUE > 0)",
            domain.buildCreateDDL());
    }

    @Test
    public void createDDLCombinesEveryOptionalClauseInOrder() {
        MimerDomain domain = new MimerDomain(schema, "positive_int");
        domain.setDataType("INTEGER");
        domain.setCollation("\"s\".\"c\"");
        domain.setDefaultValue("1");
        domain.setConstraintName("chk");
        domain.setCheckClause("VALUE > 0");

        Assertions.assertEquals(
            "CREATE DOMAIN \"mimer_store\".\"positive_int\" AS\n" +
            "INTEGER COLLATE \"s\".\"c\"\n" +
            "DEFAULT 1\n" +
            "CONSTRAINT \"chk\" CHECK(VALUE > 0)",
            domain.buildCreateDDL());
    }
}
