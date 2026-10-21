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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ResultSetCellDragTextTest extends DBeaverUnitTest {

    @Test
    public void joinSqlLiteralsEmpty() {
        Assertions.assertEquals("", ResultSetCellDragText.joinSqlLiterals(List.of()));
    }

    @Test
    public void joinSqlLiteralsSingleAndMultiple() {
        Assertions.assertEquals("'a'", ResultSetCellDragText.joinSqlLiterals(List.of("'a'")));
        Assertions.assertEquals("1, 2, NULL", ResultSetCellDragText.joinSqlLiterals(List.of("1", "2", "NULL")));
    }

    @Test
    public void joinDisplayRowsEmpty() {
        Assertions.assertEquals("", ResultSetCellDragText.joinDisplayRows(List.of()));
    }

    @Test
    public void joinDisplayRowsSeparatorsAndNullLabel() {
        Assertions.assertEquals(
            "a, " + DBConstants.NULL_VALUE_LABEL + "\nb, c",
            ResultSetCellDragText.joinDisplayRows(List.of(
                List.of("a", DBConstants.NULL_VALUE_LABEL),
                List.of("b", "c")
            ))
        );
    }

    @Test
    public void joinDisplayRowsSingleCell() {
        Assertions.assertEquals("only", ResultSetCellDragText.joinDisplayRows(List.of(List.of("only"))));
    }

    @Test
    public void joinDisplayRowsJavaNullMatchesStringBuilderAppend() {
        // Row-header drag uses StringBuilder.append(Object); String.valueOf((Object) null) == "null".
        Assertions.assertEquals(
            "null, x",
            ResultSetCellDragText.joinDisplayRows(List.of(List.of(String.valueOf((Object) null), "x")))
        );
    }
}
