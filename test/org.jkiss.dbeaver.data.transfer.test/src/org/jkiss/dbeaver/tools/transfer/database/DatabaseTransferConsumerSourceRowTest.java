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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.transformers.DataTransferTransformerExpression;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

class DatabaseTransferConsumerSourceRowTest extends DBeaverUnitTest {

    private final DBDAttributeBinding id = mockAttribute("id", 0);
    private final DBDAttributeBinding dataToDrop = mockAttribute("dataToDrop", 1);
    private final DBDAttributeBinding country = mockAttribute("country", 2);
    private final DBDAttributeBinding fkRelation = mockAttribute("fk_relation", 3);
    private final DBDAttributeBinding[] sourceAttributes = {id, dataToDrop, country, fkRelation};

    @Test
    void sourceRowKeepsSourcePositionsWhenColumnIsSkipped() {
        Object[] sourceRow = DatabaseTransferConsumer.makeSourceRow(
            sourceAttributes,
            mappingsWithSkippedColumn(),
            new Object[]{1, "NL", 8}
        );

        Assertions.assertArrayEquals(new Object[]{1, null, "NL", 8}, sourceRow);
    }

    @Test
    void expressionReadsColumnByNameWhenColumnIsSkipped() throws Exception {
        Object[] sourceRow = DatabaseTransferConsumer.makeSourceRow(
            sourceAttributes,
            mappingsWithSkippedColumn(),
            new Object[]{1, "NL", 8}
        );

        Object value = new DataTransferTransformerExpression().transformAttribute(
            Mockito.mock(DBCSession.class),
            sourceAttributes,
            sourceRow,
            country,
            "NL",
            Map.<String, Object>of("expression", "country")
        );

        Assertions.assertEquals("NL", value);
    }

    @NotNull
    private DatabaseTransferConsumer.ColumnMapping[] mappingsWithSkippedColumn() {
        return new DatabaseTransferConsumer.ColumnMapping[]{
            mapping(id, 0),
            null,
            mapping(country, 1),
            mapping(fkRelation, 2)
        };
    }

    @NotNull
    private static DatabaseTransferConsumer.ColumnMapping mapping(@NotNull DBDAttributeBinding sourceAttr, int targetIndex) {
        DatabaseTransferConsumer.ColumnMapping mapping = new DatabaseTransferConsumer.ColumnMapping(sourceAttr);
        mapping.targetIndex = targetIndex;
        return mapping;
    }

    @NotNull
    private static DBDAttributeBinding mockAttribute(@NotNull String name, int position) {
        DBDAttributeBinding attribute = Mockito.mock(DBDAttributeBinding.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(name).when(attribute).getName();
        Mockito.doReturn(position).when(attribute).getOrdinalPosition();
        Mockito.doReturn(null).when(attribute).getParentObject();
        return attribute;
    }
}
