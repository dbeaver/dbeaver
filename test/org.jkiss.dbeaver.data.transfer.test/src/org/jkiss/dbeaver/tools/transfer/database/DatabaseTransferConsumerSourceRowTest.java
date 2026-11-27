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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferResultSet;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamDataSource;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamDataSourceContainer;
import org.jkiss.dbeaver.tools.transfer.transformers.DataTransferTransformerExpression;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DatabaseTransferConsumerSourceRowTest extends DBeaverUnitTest {

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
            new Object[]{1, "NL", 8},
            new Object[]{null, "ignored", null, null}
        );

        Assertions.assertArrayEquals(new Object[]{1, "ignored", "NL", 8}, sourceRow);
    }

    @Test
    void expressionReadsColumnByNameWhenColumnIsSkipped() throws Exception {
        Object[] sourceRow = DatabaseTransferConsumer.makeSourceRow(
            sourceAttributes,
            mappingsWithSkippedColumn(),
            new Object[]{1, "NL", 8},
            new Object[]{null, "ignored", null, null}
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

    @ParameterizedTest
    @CsvSource({"true,true,false", "false,true,false", "true,false,false", "false,false,false", "true,true,true", "false,true,true"})
    void skippedColumnsAreAvailableToExpressionsButExcludedFromOutput(boolean preview, boolean transform, boolean sharedOrdinal) throws Exception {
        StreamEntityMapping source = new StreamEntityMapping(Path.of("source.csv"));
        String[] names = {"id", "dataToDrop", "country", "fk_relation"};
        for (int i = 0; i < names.length; i++) {
            source.getStreamColumns().add(new StreamDataImporterColumnInfo(
                source, sharedOrdinal && i == 3 ? 2 : i, names[i], "VARCHAR", 100, DBPDataKind.STRING));
        }
        StreamDataSourceContainer targetContainer = Mockito.spy((StreamDataSourceContainer) new StreamDataSource("target").getContainer());
        Mockito.doReturn(true).when(targetContainer).hasModifyPermission(DBPDataSourcePermission.PERMISSION_IMPORT_DATA);
        StreamDataSource targetDataSource = new StreamDataSource(targetContainer);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(target.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator.ExecuteBatch batch = Mockito.mock(DBSDataManipulator.ExecuteBatch.class);
        Mockito.when(target.insertData(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any(), Mockito.any())).thenReturn(batch);
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.setTarget(target);
        mapping.setMappingType(DatabaseMappingType.existing);
        settings.getDataMappings().put(source, mapping);
        for (DatabaseMappingAttribute attribute : mapping.getAttributeMappings()) {
            int index = attribute.getSource().getOrdinalPosition();
            String columnName = attribute.getSource().getName();
            attribute.setMappingType(index < 2 ? DatabaseMappingType.skip : DatabaseMappingType.existing);
            attribute.setTarget((StreamDataImporterColumnInfo) attribute.getSource());
            if (transform && index >= 2 && (!sharedOrdinal || columnName.equals("country"))) {
                attribute.setTransformer(DataTransferRegistry.getInstance().getAttributeTransformer("expression"));
                String expression = columnName.equals("country") ? "'TSO_' + id" : "country + '_tail'";
                attribute.setTransformerProperties(Map.of("expression", expression));
            }
        }
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer(target);
        consumer.setPreview(preview);
        consumer.initTransfer(source, settings, new IDataTransferConsumer.TransferParameters(), null, null, null);
        try (DBCSession sourceSession = source.getDataSource().getDefaultInstance()
            .getDefaultContext(new VoidProgressMonitor(), false).openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Test")) {
            StreamTransferResultSet rows = Mockito.spy(new StreamTransferResultSet(sourceSession, Mockito.mock(DBCStatement.class), source));
            rows.setStreamRow(new Object[]{1, "ignored", "NL", 8});
            consumer.fetchStart(sourceSession, rows, 0, 2);
            if (sharedOrdinal) {
                var column = consumer.getColumnMappings()[3];
                column.sourceValueHandler = Mockito.mock(DBDValueHandler.class);
                Mockito.when(column.sourceValueHandler.fetchValueObject(sourceSession, rows, column.sourceAttr, 2))
                    .thenAnswer(invocation -> rows.getAttributeValue(3));
            }
            Mockito.clearInvocations(rows);
            consumer.fetchRow(sourceSession, rows);
            rows.setStreamRow(new Object[]{2, "ignored again", "IE", 9});
            consumer.fetchRow(sourceSession, rows);

            Assertions.assertEquals(List.of("country", "fk_relation"), consumer.getTargetAttributes().stream()
                .map(DBSAttributeBase::getName).toList());
            List<Object[]> output;
            if (preview) {
                output = consumer.getPreviewRows();
            } else {
                ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
                Mockito.verify(batch, Mockito.times(2)).add(captor.capture());
                output = captor.getAllValues();
            }
            Assertions.assertArrayEquals(transform ? new Object[]{"TSO_1", sharedOrdinal ? 8 : "TSO_1_tail"} : new Object[]{"NL", 8}, output.get(0));
            Assertions.assertArrayEquals(transform ? new Object[]{"TSO_2", sharedOrdinal ? 9 : "TSO_2_tail"} : new Object[]{"IE", 9}, output.get(1));
            for (int i = 0; i < names.length; i++) {
                Mockito.verify(rows, Mockito.times(i < 2 && !transform ? 0 : 2)).getAttributeValue(i);
            }
            var order = Mockito.inOrder(rows);
            for (int row = 0; row < 2; row++) {
                for (int i = transform ? 0 : 2; i < names.length; i++) {
                    order.verify(rows).getAttributeValue(i);
                }
            }
            if (transform) {
                DBDValue skippedValue = Mockito.mock(DBDValue.class);
                rows.setStreamRow(new Object[]{skippedValue, "ignored", "unused", 0});
                Mockito.doThrow(new DBCException("Read failed")).when(rows).getAttributeValue(1);
                Assertions.assertThrows(DBCException.class, () -> consumer.fetchRow(sourceSession, rows));
                Mockito.verify(skippedValue).release();
            }
            consumer.fetchEnd(sourceSession, rows);
        } finally {
            consumer.close();
        }
    }

    @Test
    void skippedValuesAreReleasedUnlessTransferredToTarget() {
        DBDValue unused = Mockito.mock(DBDValue.class);
        DBDValue transferred = Mockito.mock(DBDValue.class);

        DatabaseTransferConsumer.releaseSkippedSourceValues(new Object[]{unused, unused, transferred}, new Object[]{transferred});

        Mockito.verify(unused).release();
        Mockito.verify(transferred, Mockito.never()).release();
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
