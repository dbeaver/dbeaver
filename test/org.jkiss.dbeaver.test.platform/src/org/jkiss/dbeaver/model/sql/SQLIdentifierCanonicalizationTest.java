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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Verifies all identifier storage-case combinations allowed by the JDBC {@link java.sql.DatabaseMetaData} contract.
 * Unquoted cases correspond to {@code storesUpperCaseIdentifiers()}, {@code storesLowerCaseIdentifiers()}, or the
 * mixed-case fallback. Quoted cases independently correspond to the matching {@code *QuotedIdentifiers()} methods.
 * Prepared input represents an identifier already obtained from database metadata, while forced-unquoted output is
 * the form suitable for direct metadata-cache lookup.
 */
public class SQLIdentifierCanonicalizationTest extends DBeaverUnitTest {
    // LOWER/LOWER: TDengine declares this combination; generic drivers can configure it explicitly.
    private static final IdentifierBehavior LOWER_LOWER = new IdentifierBehavior(
        "LOWER/LOWER (TDengine)",
        dialect(DBPIdentifierCase.LOWER, DBPIdentifierCase.LOWER, new String[][]{{"`", "`"}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("myschema", "myschema", "myschema", "myschema", "myschema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "myschema", "myschema", "`MySchema`", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("`MySchema`", "myschema", "myschema", "`MySchema`", "`MySchema`"),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("`My``Schema`", "`my``schema`", "my`schema", "`My``Schema`", "`My``Schema`"),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("`name with space`", "`name with space`", "name with space", "`name with space`", "`name with space`")
        )
    );
    // LOWER/UPPER: legal JDBC metadata and generic-driver configuration, with no common built-in driver example.
    private static final IdentifierBehavior LOWER_UPPER = new IdentifierBehavior(
        "LOWER/UPPER (JDBC or generic)",
        dialect(DBPIdentifierCase.LOWER, DBPIdentifierCase.UPPER, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("myschema", "myschema", "myschema", "myschema", "myschema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "myschema", "myschema", "\"MySchema\"", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "\"MYSCHEMA\"", "MYSCHEMA", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"MY\"\"SCHEMA\"", "MY\"SCHEMA", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"NAME WITH SPACE\"", "NAME WITH SPACE", "\"name with space\"", "\"name with space\"")
        )
    );
    // LOWER/MIXED: PostgreSQL folds unquoted names to lower case and preserves quoted spelling.
    private static final IdentifierBehavior LOWER_MIXED = new IdentifierBehavior(
        "LOWER/MIXED (PostgreSQL)",
        dialect(DBPIdentifierCase.LOWER, DBPIdentifierCase.MIXED, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("myschema", "myschema", "myschema", "myschema", "myschema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "myschema", "myschema", "\"MySchema\"", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "\"MySchema\"", "MySchema", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"My\"\"Schema\"", "My\"Schema", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"name with space\"", "name with space", "\"name with space\"", "\"name with space\"")
        )
    );
    // UPPER/LOWER: legal JDBC metadata and generic-driver configuration, with no common built-in driver example.
    private static final IdentifierBehavior UPPER_LOWER = new IdentifierBehavior(
        "UPPER/LOWER (JDBC or generic)",
        dialect(DBPIdentifierCase.UPPER, DBPIdentifierCase.LOWER, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "MYSCHEMA", "MYSCHEMA", "\"MySchema\"", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "\"myschema\"", "myschema", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"my\"\"schema\"", "my\"schema", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"name with space\"", "name with space", "\"name with space\"", "\"name with space\"")
        )
    );
    // UPPER/UPPER: uncommon but directly reportable by JDBC metadata and configurable for generic drivers.
    private static final IdentifierBehavior UPPER_UPPER = new IdentifierBehavior(
        "UPPER/UPPER (JDBC or generic)",
        dialect(DBPIdentifierCase.UPPER, DBPIdentifierCase.UPPER, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "MYSCHEMA", "MYSCHEMA", "\"MySchema\"", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "MYSCHEMA", "MYSCHEMA", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"MY\"\"SCHEMA\"", "MY\"SCHEMA", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"NAME WITH SPACE\"", "NAME WITH SPACE", "\"name with space\"", "\"name with space\"")
        )
    );
    // UPPER/MIXED: Oracle and typical DB2 drivers fold unquoted names to upper case and preserve quoted spelling.
    private static final IdentifierBehavior UPPER_MIXED = new IdentifierBehavior(
        "UPPER/MIXED (Oracle and DB2)",
        dialect(DBPIdentifierCase.UPPER, DBPIdentifierCase.MIXED, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA", "MYSCHEMA"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MySchema", "MYSCHEMA", "MYSCHEMA", "\"MySchema\"", "MySchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "\"MySchema\"", "MySchema", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"My\"\"Schema\"", "My\"Schema", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"name with space\"", "name with space", "\"name with space\"", "\"name with space\"")
        )
    );
    // MIXED/LOWER: legal JDBC metadata and generic-driver configuration, with no common built-in driver example.
    private static final IdentifierBehavior MIXED_LOWER = new IdentifierBehavior(
        "MIXED/LOWER (JDBC or generic)",
        dialect(DBPIdentifierCase.MIXED, DBPIdentifierCase.LOWER, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MySchema", "MySchema", "MySchema", "MySchema", "MySchema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "myschema", "myschema", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"my\"\"schema\"", "my\"schema", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"name with space\"", "name with space", "\"name with space\"", "\"name with space\"")
        )
    );
    // MIXED/UPPER: legal JDBC metadata and generic-driver configuration, with no common built-in driver example.
    private static final IdentifierBehavior MIXED_UPPER = new IdentifierBehavior(
        "MIXED/UPPER (JDBC or generic)",
        dialect(DBPIdentifierCase.MIXED, DBPIdentifierCase.UPPER, new String[][]{{"\"", "\""}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MySchema", "MySchema", "MySchema", "MySchema", "MySchema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("\"MySchema\"", "MYSCHEMA", "MYSCHEMA", "\"MySchema\"", "\"MySchema\""),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("\"My\"\"Schema\"", "\"MY\"\"SCHEMA\"", "MY\"SCHEMA", "\"My\"\"Schema\"", "\"My\"\"Schema\""),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("\"name with space\"", "\"NAME WITH SPACE\"", "NAME WITH SPACE", "\"name with space\"", "\"name with space\"")
        )
    );
    // MIXED/MIXED: typical case-insensitive SQL Server configurations preserve both forms; generic drivers support it.
    private static final IdentifierBehavior MIXED_MIXED = new IdentifierBehavior(
        "MIXED/MIXED (SQL Server)",
        dialect(DBPIdentifierCase.MIXED, DBPIdentifierCase.MIXED, new String[][]{{"[", "]"}}),
        Map.of(
            ScenarioKind.STORED, new IdentifierForms("MySchema", "MySchema", "MySchema", "MySchema", "MySchema"),
            ScenarioKind.UNQUOTED_MIXED, new IdentifierForms("MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema", "MixedSchema"),
            ScenarioKind.QUOTED_MIXED, new IdentifierForms("[MySchema]", "MySchema", "MySchema", "[MySchema]", "[MySchema]"),
            ScenarioKind.ESCAPED_QUOTED, new IdentifierForms("[My]]Schema]", "[My]]Schema]", "My]Schema", "[My]]Schema]", "[My]]Schema]"),
            ScenarioKind.REQUIRES_QUOTES, new IdentifierForms("[name with space]", "[name with space]", "name with space", "[name with space]", "[name with space]")
        )
    );

    private static final List<IdentifierBehavior> ALL_BEHAVIORS = List.of(
        LOWER_LOWER,
        LOWER_UPPER,
        LOWER_MIXED,
        UPPER_LOWER,
        UPPER_UPPER,
        UPPER_MIXED,
        MIXED_LOWER,
        MIXED_UPPER,
        MIXED_MIXED
    );

    @Test
    void identifiersInStoredCase() {
        assertIdentifierScenario(ScenarioKind.STORED);
    }

    @Test
    void unquotedMixedCaseIdentifiers() {
        assertIdentifierScenario(ScenarioKind.UNQUOTED_MIXED);
    }

    @Test
    void quotedMixedCaseIdentifiers() {
        assertIdentifierScenario(ScenarioKind.QUOTED_MIXED);
    }

    @Test
    void escapedQuotedIdentifiers() {
        assertIdentifierScenario(ScenarioKind.ESCAPED_QUOTED);
    }

    @Test
    void identifiersRequiringQuotes() {
        assertIdentifierScenario(ScenarioKind.REQUIRES_QUOTES);
    }

    private static void assertIdentifierScenario(@NotNull ScenarioKind kind) {
        for (IdentifierBehavior behavior : ALL_BEHAVIORS) {
            IdentifierForms identifier = behavior.forms().get(kind);
            Assertions.assertAll(
                behavior.name() + ": " + identifier.input(),
                () -> Assertions.assertEquals(
                    identifier.canonical(),
                    SQLUtils.identifierToCanonicalForm(behavior.dialect(), identifier.input(), false, false),
                    "source canonical form"
                ),
                () -> Assertions.assertEquals(
                    identifier.unquotedCanonical(),
                    SQLUtils.identifierToCanonicalForm(behavior.dialect(), identifier.input(), true, false),
                    "source unquoted canonical form"
                ),
                () -> Assertions.assertEquals(
                    identifier.preparedCanonical(),
                    SQLUtils.identifierToCanonicalForm(behavior.dialect(), identifier.input(), false, true),
                    "prepared canonical form"
                ),
                () -> Assertions.assertEquals(
                    identifier.preparedUnquoted(),
                    SQLUtils.identifierToCanonicalForm(behavior.dialect(), identifier.input(), true, true),
                    "prepared unquoted form"
                )
            );
        }
    }

    @NotNull
    private static SQLDialect dialect(
        @NotNull DBPIdentifierCase unquotedCase,
        @NotNull DBPIdentifierCase quotedCase,
        @NotNull String[][] identifierQuotes
    ) {
        return new BasicSQLDialect() {
            @NotNull
            @Override
            public DBPIdentifierCase storesUnquotedCase() {
                return unquotedCase;
            }

            @NotNull
            @Override
            public DBPIdentifierCase storesQuotedCase() {
                return quotedCase;
            }

            @NotNull
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return identifierQuotes;
            }

            @NotNull
            @Override
            protected String quoteIdentifier(@NotNull String identifier, @NotNull String[][] quoteStrings) {
                String[] quotes = quoteStrings[0];
                return quotes[0] + identifier.replace(quotes[1], quotes[1] + quotes[1]) + quotes[1];
            }
        };
    }

    private record IdentifierForms(
        @NotNull String input,
        @NotNull String canonical,
        @NotNull String unquotedCanonical,
        @NotNull String preparedCanonical,
        @NotNull String preparedUnquoted
    ) {
    }

    private record IdentifierBehavior(
        @NotNull String name,
        @NotNull SQLDialect dialect,
        @NotNull Map<ScenarioKind, IdentifierForms> forms
    ) {
    }

    private enum ScenarioKind {
        STORED,
        UNQUOTED_MIXED,
        QUOTED_MIXED,
        ESCAPED_QUOTED,
        REQUIRES_QUOTES
    }
}
