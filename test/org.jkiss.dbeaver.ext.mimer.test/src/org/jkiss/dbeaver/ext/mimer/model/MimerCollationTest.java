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
 * {@link MimerCollation#buildCreateDDL()} - {@code CREATE COLLATION ... FROM ... [USING '...']}.
 * A new collation is always based on an existing one (see {@code CREATE_COLLATION.htm}, confirmed
 * in the class Javadoc) - there's no "from scratch" form to test.
 *
 * @author Mimer Information Technology
 */
public class MimerCollationTest extends DBeaverUnitTest {

    @Mock
    private MimerSchema schema;

    @BeforeEach
    public void setUp() {
        when(schema.getName()).thenReturn("mimer_store");
    }

    @Test
    public void createDDLWithNoUsingClauseIsAnExactCopyOfTheSource() {
        MimerCollation collation = new MimerCollation(schema, "my_collation");
        collation.setSourceCollation("\"INFORMATION_SCHEMA\".\"ISO8BIT\"");

        Assertions.assertEquals(
            "CREATE COLLATION \"mimer_store\".\"my_collation\"\nFROM \"INFORMATION_SCHEMA\".\"ISO8BIT\"",
            collation.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesTheUsingDeltaStringWhenSet() {
        MimerCollation collation = new MimerCollation(schema, "my_collation");
        collation.setSourceCollation("\"INFORMATION_SCHEMA\".\"ISO8BIT\"");
        collation.setUsingClause("some delta");

        Assertions.assertEquals(
            "CREATE COLLATION \"mimer_store\".\"my_collation\"\nFROM \"INFORMATION_SCHEMA\".\"ISO8BIT\"\nUSING 'some delta'",
            collation.buildCreateDDL());
    }

    @Test
    public void createDDLEscapesASingleQuoteInTheUsingDelta() {
        MimerCollation collation = new MimerCollation(schema, "my_collation");
        collation.setSourceCollation("\"s\".\"c\"");
        collation.setUsingClause("O'Brien's delta");

        Assertions.assertTrue(collation.buildCreateDDL().endsWith("USING 'O''Brien''s delta'"));
    }

    /**
     * {@link MimerCollation#getReference()}/{@link MimerCollation#formatReference} - a bare,
     * unquoted name for one of Mimer SQL's two built-in {@code INFORMATION_SCHEMA} collations
     * (schema match is case-insensitive, matching how {@code MimerUtils#buildDomainSource}
     * already treats it), the usual quoted {@code "schema"."name"} form for anything else.
     */
    @Test
    public void referenceIsBareForAnInformationSchemaCollation() {
        when(schema.getName()).thenReturn("INFORMATION_SCHEMA");
        MimerCollation collation = new MimerCollation(schema, "ISO8BIT");

        Assertions.assertEquals("ISO8BIT", collation.getReference());
    }

    @Test
    public void referenceIsCaseInsensitiveForInformationSchema() {
        when(schema.getName()).thenReturn("information_schema");
        MimerCollation collation = new MimerCollation(schema, "UCS_BASIC");

        Assertions.assertEquals("UCS_BASIC", collation.getReference());
    }

    @Test
    public void referenceIsQuotedAndQualifiedForAnyOtherSchema() {
        MimerCollation collation = new MimerCollation(schema, "my_collation");

        Assertions.assertEquals("\"mimer_store\".\"my_collation\"", collation.getReference());
    }
}
