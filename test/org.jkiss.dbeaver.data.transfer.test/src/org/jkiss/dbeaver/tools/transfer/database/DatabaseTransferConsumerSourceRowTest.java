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
import org.jkiss.dbeaver.model.data.DBDAttributeBindingElement;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
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
import org.junit.jupiter.api.BeforeEach;
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

    private final DBCSession session = Mockito.mock(DBCSession.class);
    private final DBCResultSet resultSet = Mockito.mock(DBCResultSet.class);
    private final DBDValueHandler valueHandler = Mockito.mock(DBDValueHandler.class);

    @BeforeEach
    void configureSourceValues() throws DBCException {
        Object[] values = {1, "ignored", "NL", 8};
        for (DBDAttributeBinding attribute : sourceAttributes) {
            int index = attribute.getOrdinalPosition();
            Mockito.doReturn(valueHandler).when(attribute).getValueHandler();
            Mockito.when(valueHandler.fetchValueObject(session, resultSet, attribute, index)).thenReturn(values[index]);
        }
    }

    @Test
    void sourceRowIncludesColumnsExcludedFromTarget() throws DBCException {
        Object[] sourceRow = DatabaseTransferConsumer.readSourceValues(session, resultSet, sourceAttributes);

        Assertions.assertArrayEquals(new Object[]{1, "ignored", "NL", 8}, sourceRow);
        var order = Mockito.inOrder(valueHandler);
        for (DBDAttributeBinding attribute : sourceAttributes) {
            order.verify(valueHandler).fetchValueObject(session, resultSet, attribute, attribute.getOrdinalPosition());
        }
        order.verifyNoMoreInteractions();
    }

    @Test
    void expressionReadsColumnByNameWhenColumnIsSkipped() throws Exception {
        Object[] sourceRow = DatabaseTransferConsumer.readSourceValues(session, resultSet, sourceAttributes);

        Object value = new DataTransferTransformerExpression().transformAttribute(
            session,
            sourceAttributes,
            DatabaseTransferConsumer.makeSourceRow(sourceAttributes, sourceRow),
            country,
            "NL",
            Map.<String, Object>of("expression", "country")
        );

        Assertions.assertEquals("NL", value);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,true", "true,false", "false,false"})
    void skippedColumnsAreAvailableToExpressionsButExcludedFromOutput(boolean preview, boolean transform) throws Exception {
        StreamEntityMapping source = new StreamEntityMapping(Path.of("source.csv"));
        String[] names = {"id", "dataToDrop", "country", "fk_relation"};
        for (int i = 0; i < names.length; i++) {
            source.getStreamColumns().add(new StreamDataImporterColumnInfo(source, i, names[i], "VARCHAR", 100, DBPDataKind.STRING));
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
            attribute.setMappingType(index < 2 ? DatabaseMappingType.skip : DatabaseMappingType.existing);
            attribute.setTarget(source.getStreamColumns().get(index));
            if (transform && index >= 2) {
                attribute.setTransformer(DataTransferRegistry.getInstance().getAttributeTransformer("expression"));
                attribute.setTransformerProperties(Map.of("expression", index == 2 ? "'TSO_' + id" : "country + '_tail'"));
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
            Mockito.clearInvocations(rows);
            consumer.fetchRow(sourceSession, rows);
            rows.setStreamRow(new Object[]{2, "ignored again", "IE", 9});
            consumer.fetchRow(sourceSession, rows);
            consumer.fetchEnd(sourceSession, rows);

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
            Assertions.assertArrayEquals(transform ? new Object[]{"TSO_1", "TSO_1_tail"} : new Object[]{"NL", 8}, output.get(0));
            Assertions.assertArrayEquals(transform ? new Object[]{"TSO_2", "TSO_2_tail"} : new Object[]{"IE", 9}, output.get(1));
            for (int i = 0; i < names.length; i++) {
                Mockito.verify(rows, Mockito.times(i < 2 && !transform ? 0 : 2)).getAttributeValue(i);
            }
        } finally {
            consumer.close();
        }
    }

    @Test
    void collectionLeafValuesWithTheSameOrdinalRemainDistinct() throws DBCException {
        DBDCollection collection = Mockito.mock(DBDCollection.class);
        Mockito.when(collection.getComponentValueHandler()).thenReturn(valueHandler);
        DBDAttributeBinding first = new DBDAttributeBindingElement(id, collection, 0);
        DBDAttributeBinding second = new DBDAttributeBindingElement(id, collection, 1);
        Mockito.when(valueHandler.fetchValueObject(session, resultSet, first, 0)).thenReturn("first element");
        Mockito.when(valueHandler.fetchValueObject(session, resultSet, second, 0)).thenReturn("second element");

        Object[] sourceValues = DatabaseTransferConsumer.readSourceValues(
            session, resultSet, new DBDAttributeBinding[]{first, second}
        );

        Assertions.assertArrayEquals(new Object[]{"first element", "second element"}, sourceValues);
    }

    @Test
    void sourceRowDoesNotReadPseudoOrCustomAttributes() throws DBCException {
        Mockito.doReturn(true).when(id).isPseudoAttribute();
        Mockito.doReturn(true).when(dataToDrop).isCustom();

        Object[] sourceRow = DatabaseTransferConsumer.readSourceValues(session, resultSet, sourceAttributes);

        Assertions.assertArrayEquals(new Object[]{null, null, "NL", 8}, sourceRow);
        Mockito.verify(valueHandler, Mockito.never()).fetchValueObject(session, resultSet, id, 0);
        Mockito.verify(valueHandler, Mockito.never()).fetchValueObject(session, resultSet, dataToDrop, 1);
    }

    @Test
    void failedReadReleasesPreviouslyFetchedValues() throws DBCException {
        DBDValue value = Mockito.mock(DBDValue.class);
        Mockito.when(valueHandler.fetchValueObject(session, resultSet, id, 0)).thenReturn(value);
        Mockito.when(valueHandler.fetchValueObject(session, resultSet, country, 2)).thenThrow(new DBCException("Read failed"));

        Assertions.assertThrows(DBCException.class, () -> DatabaseTransferConsumer.readSourceValues(session, resultSet, sourceAttributes));

        Mockito.verify(value).release();
    }

    @Test
    void unusedSkippedValuesAreReleased() {
        DBDValue value = Mockito.mock(DBDValue.class);
        Object[] sourceRow = {value, value, "NL", 8};
        DatabaseTransferConsumer.ColumnMapping[] mappings = {null, null, mapping(country, 0), mapping(fkRelation, 1)};

        DatabaseTransferConsumer.releaseSkippedSourceValues(sourceAttributes, mappings, sourceRow, new Object[]{"NL", 8});

        Mockito.verify(value).release();
    }

    @Test
    void skippedValueReturnedByExpressionIsRetainedForBatch() throws Exception {
        DBDValue value = Mockito.mock(DBDValue.class);
        Object[] sourceRow = {value, "ignored", "NL", 8};
        DatabaseTransferConsumer.ColumnMapping[] mappings = {null, null, mapping(country, 0), mapping(fkRelation, 1)};
        Object transformed = new DataTransferTransformerExpression().transformAttribute(
            session, sourceAttributes, DatabaseTransferConsumer.makeSourceRow(sourceAttributes, sourceRow),
            country, "NL", Map.of("expression", "id")
        );

        DatabaseTransferConsumer.releaseSkippedSourceValues(sourceAttributes, mappings, sourceRow, new Object[]{transformed, 8});

        Assertions.assertSame(value, transformed);
        Mockito.verify(value, Mockito.never()).release();
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
