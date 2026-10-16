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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreProcedureAggregateDDLTest {

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
