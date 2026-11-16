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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class PostgreProcedureAggregateDDLTest {

    @Test
    public void legacyAggregateQueryUsesOidAndSupportedCatalogColumns() {
        String query = getQuery(8, 2);

        Assertions.assertTrue(query.endsWith("WHERE aggfnoid = ?"));
        Assertions.assertTrue(query.contains("agginitval\nFROM pg_aggregate"));
        Assertions.assertTrue(query.contains("CASE aggfinalfn WHEN 0 THEN NULL ELSE aggfinalfn END AS aggfinalfn"));
        Assertions.assertFalse(query.contains("aggfinalfn::text"));
        for (String unsupported : List.of("pg_identify_object", "aggmtransfn", "aggserialfn", "aggcombinefn", "aggfinalmodify")) {
            Assertions.assertFalse(query.contains(unsupported), unsupported);
        }
    }

    @Test
    public void objectIdentityIsAvailableBeforeMovingAggregates() {
        String query = getQuery(9, 3);

        Assertions.assertTrue(query.contains("pg_identify_object"));
        Assertions.assertFalse(query.contains("aggmtransfn"));
        Assertions.assertFalse(query.contains(",\nFROM"));
    }

    @Test
    public void movingAggregatesDoNotRequireSerializationSupport() {
        for (int minor : List.of(4, 5)) {
            String query = getQuery(9, minor);
            Assertions.assertTrue(query.contains("aggmtransfn"));
            Assertions.assertFalse(query.contains("aggserialfn"));
            Assertions.assertFalse(query.contains("aggdeserialfn"));
            Assertions.assertFalse(query.contains("proparallel"));
        }
    }

    @Test
    public void parallelAggregatesIncludeSerializationAndCombineFunctions() {
        String query = getQuery(9, 6);

        for (String column : List.of("aggserialfn", "aggdeserialfn", "aggcombinefn", "pg_proc.proparallel")) {
            Assertions.assertTrue(query.contains(column), column);
        }
        Assertions.assertFalse(query.contains("aggfinalmodify"));
        Assertions.assertTrue(getQuery(11, 0).contains("aggfinalmodify"));
    }

    @NotNull
    private String getQuery(int major, int minor) {
        PostgreDataSource dataSource = Mockito.mock(PostgreDataSource.class);
        Mockito.when(dataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
            int requiredMajor = invocation.getArgument(0);
            int requiredMinor = invocation.getArgument(1);
            return major > requiredMajor || major == requiredMajor && minor >= requiredMinor;
        });
        return PostgreProcedure.getAggregateQuery(dataSource);
    }

    @Test
    public void preservesCombineFunctionAndParallelSafe() {
        StringBuilder ddl = new StringBuilder("SFUNC = state_cat");
        PostgreProcedure.appendAggregateExecutionOptions(ddl, "agg_test.state_combine", "s");

        Assertions.assertEquals(
            "SFUNC = state_cat,\n\tCOMBINEFUNC = agg_test.state_combine,\n\tPARALLEL = SAFE",
            ddl.toString()
        );
    }

    @Test
    public void preservesRestrictedParallelismWithoutCombineFunction() {
        StringBuilder ddl = new StringBuilder("STYPE = anyarray");
        PostgreProcedure.appendAggregateExecutionOptions(ddl, "-", "r");

        Assertions.assertEquals("STYPE = anyarray,\n\tPARALLEL = RESTRICTED", ddl.toString());
    }

    @Test
    public void omitsDefaultUnsafeParallelismAndMissingCombineFunction() {
        StringBuilder ddl = new StringBuilder("STYPE = bigint");
        PostgreProcedure.appendAggregateExecutionOptions(ddl, null, "u");

        Assertions.assertEquals("STYPE = bigint", ddl.toString());
    }

    @Test
    public void preservesCombineFunctionWithoutParallelMetadataOnOlderServers() {
        StringBuilder ddl = new StringBuilder("STYPE = bigint");
        PostgreProcedure.appendAggregateExecutionOptions(ddl, "agg_test.combine", null);

        Assertions.assertEquals("STYPE = bigint,\n\tCOMBINEFUNC = agg_test.combine", ddl.toString());
    }
}
