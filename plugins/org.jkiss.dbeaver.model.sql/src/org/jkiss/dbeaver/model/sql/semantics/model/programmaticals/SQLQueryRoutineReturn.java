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
package org.jkiss.dbeaver.model.sql.semantics.model.programmaticals;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/** The syntactic return form, independent of OUT parameters in the routine signature. */
public sealed interface SQLQueryRoutineReturn {
    record None() implements SQLQueryRoutineReturn {
    }

    record Incomplete(@NotNull STMTreeNode syntax) implements SQLQueryRoutineReturn {
    }

    record Scalar(
        @NotNull SQLQueryModelRecognizer.TypeReference reference,
        @Nullable SQLQueryExprType resolvedType
    ) implements SQLQueryRoutineReturn {
    }

    /** Columns of a table-shaped result; names do not necessarily bind inside the body. */
    record TableColumns(@NotNull List<ResultField> fields) implements SQLQueryRoutineReturn {
        public TableColumns {
            fields = List.copyOf(fields);
        }
    }

    /** Output parameters declared by a RETURNS (...) clause, as in Firebird PSQL. */
    record OutputParameters(@NotNull List<ResultField> fields) implements SQLQueryRoutineReturn {
        public OutputParameters {
            fields = List.copyOf(fields);
        }
    }

    record ResultField(
        @Nullable SQLQuerySymbolEntry name,
        @NotNull SQLQueryModelRecognizer.TypeReference reference,
        @Nullable SQLQueryExprType resolvedType
    ) {
    }
}
