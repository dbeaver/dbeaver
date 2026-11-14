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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionDummyContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionRealContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableClause;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableAlterModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsLocalTableModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.*;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSTypeDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx2;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.util.*;

public class SQLQueryModelRecognizerTest extends DBeaverUnitTest {

    @Test
    void capturedTypeLookupOrderAndDistinctBaseFallback() throws Exception {
        var monitor = new VoidProgressMonitor();
        var connection = Mockito.mock(SQLQueryConnectionContext.class);
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(statistics.getMonitor()).thenReturn(monitor);
        var reference = typeReference("numeric(10,2)", null, true, "UNSIGNED");
        var metadata = Mockito.mock(DBSDataType.class);
        Mockito.when(metadata.getFullTypeName()).thenReturn("numeric");
        Mockito.when(metadata.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(connection.resolveDataType(monitor, "numeric(10,2) UNSIGNED")).thenThrow(new DBException("unsupported"));
        Mockito.when(connection.resolveDataType(monitor, "numeric")).thenReturn(metadata);
        var resolved = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
        Assertions.assertSame(metadata, resolved.getTypedDbObject());
        Assertions.assertEquals(List.of("UNSIGNED"), resolved.getModifiers());
        Assertions.assertEquals("numeric(10,2) UNSIGNED", resolved.getDisplayName());
        var order = Mockito.inOrder(connection);
        order.verify(connection).isDummy();
        order.verify(connection).resolveDataType(monitor, "numeric(10,2) UNSIGNED");
        order.verify(connection).resolveDataType(monitor, "numeric(10,2)");
        order.verify(connection).resolveDataType(monitor, "numeric");
        order.verifyNoMoreInteractions();

        Mockito.clearInvocations(connection);
        SQLQuerySemanticUtils.resolveTypeReference(context, statistics,
            typeReference("numeric", null, false));
        Mockito.verify(connection).resolveDataType(monitor, "numeric");
        Mockito.verify(connection).isDummy();
        Mockito.verifyNoMoreInteractions(connection);
    }

    @Test
    void capturedQuotedPartsAndStructuralTypeSpellings() throws Exception {
        var reference = recognizedType("\"app.with.dot\".\"My Type\"(10)");
        Assertions.assertEquals(List.of("\"app.with.dot\"", "\"My Type\""), reference.nameParts());
        Assertions.assertEquals("\"app.with.dot\".\"My Type\"", reference.baseSpelling());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> reference.nameParts().clear());
        Assertions.assertEquals("TIMESTAMP WITH TIME ZONE",
            recognizedType("TIMESTAMP(6) WITH TIME ZONE").baseSpelling());
        Assertions.assertEquals("CHARACTER VARYING",
            recognizedType("CHARACTER VARYING(12)").baseSpelling());
    }

    @Test
    void innerTypeModifiersRetainPositionAndStructuralName() throws Exception {
        for (String spelling : List.of("CHARACTER VARYING(20)", "LONG RAW", "DOUBLE PRECISION")) {
            var reference = recognizedType(spelling);
            Assertions.assertEquals(spelling, reference.fullSpelling());
            Assertions.assertEquals(spelling.substring(0, spelling.indexOf(' '))
                + (spelling.contains("(") ? spelling.substring(spelling.indexOf('(')) : ""), reference.spelling());
            Assertions.assertEquals(List.of(spelling.substring(spelling.indexOf(' ') + 1).split("\\(")[0]), reference.modifiers());
            Assertions.assertEquals(List.of(spelling.split("\\(")[0]), reference.nameParts(),
                "base=" + reference.baseSpelling() + ", syntax=" + reference.syntax().getTextContent());
            var context = Mockito.mock(SQLQueryRowsSourceContext.class);
            var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
            Assertions.assertEquals(spelling,
                SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference).getDisplayName());
        }
        var quoted = recognizedType("\"LONG\" RAW");
        Assertions.assertEquals(List.of("\"LONG\""), quoted.nameParts(),
            "base=" + quoted.baseSpelling() + ", qualified=" + (quoted.qualifiedName() == null ? null : quoted.qualifiedName().stringParts));
        Assertions.assertEquals(List.of("APP", "LONG"), recognizedType("app.long RAW").nameParts());
    }

    @Test
    void providerTriesCompleteInnerModifierSignatureBeforeBase() throws Exception {
        var monitor = new VoidProgressMonitor();
        var connection = Mockito.mock(SQLQueryConnectionContext.class);
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(statistics.getMonitor()).thenReturn(monitor);
        var reference = recognizedType("CHARACTER VARYING(20)");
        var metadata = Mockito.mock(DBSDataType.class);
        Mockito.when(metadata.getFullTypeName()).thenReturn("CHARACTER VARYING");
        Mockito.when(metadata.getDataKind()).thenReturn(DBPDataKind.STRING);
        Mockito.when(connection.resolveDataType(monitor, "CHARACTER VARYING")).thenReturn(metadata);
        var resolved = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
        Assertions.assertSame(metadata, resolved.getTypedDbObject());
        Assertions.assertEquals("CHARACTER VARYING(20)", resolved.getDisplayName());
        var order = Mockito.inOrder(connection);
        order.verify(connection).isDummy();
        order.verify(connection).resolveDataType(monitor, "CHARACTER VARYING(20)");
        order.verify(connection).resolveDataType(monitor, "CHARACTER(20)");
        order.verify(connection).resolveDataType(monitor, "CHARACTER VARYING");
        Assertions.assertEquals(List.of("CHARACTER VARYING"), reference.nameParts());

        Mockito.clearInvocations(connection);
        Mockito.when(connection.resolveDataType(monitor, "CHARACTER VARYING(20)")).thenReturn(metadata);
        SQLQuerySemanticUtils.resolveTypeReference(context, statistics, recognizedType("CHARACTER VARYING(20)"));
        Mockito.verify(connection).resolveDataType(monitor, "CHARACTER VARYING(20)");
        Mockito.verify(connection, Mockito.never()).resolveDataType(monitor, "CHARACTER VARYING");
    }

    @Test
    void dummyAndMetadataDisabledTypeReferencesNeverLookup() throws Exception {
        var connection = Mockito.mock(SQLQueryConnectionContext.class);
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        var reference = typeReference("numeric(10,2)", null, true, "UNSIGNED", "ZEROFILL");
        Assertions.assertEquals("numeric(10,2) UNSIGNED ZEROFILL",
            SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference).getDisplayName());
        Mockito.verifyNoInteractions(connection);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(connection.isDummy()).thenReturn(true);
        Assertions.assertEquals(reference.modifiers(), SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference).getModifiers());
        Mockito.verify(connection).isDummy();
        Mockito.verifyNoMoreInteractions(connection);
    }

    @Test
    void structuralLookupUsesOnlyCompleteCapturedNames() throws Exception {
        var monitor = new VoidProgressMonitor();
        var connection = Mockito.mock(SQLQueryConnectionContext.class);
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(statistics.getMonitor()).thenReturn(monitor);
        var reference = typeReference("\"app.with.dot\".\"My Type\"(10)", "\"app.with.dot\".\"My Type\"", true);
        SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
        var order = Mockito.inOrder(connection);
        order.verify(connection).isDummy();
        order.verify(connection).resolveDataType(monitor, reference.spelling());
        order.verify(connection).resolveDataType(monitor, reference.baseSpelling());
        var lookups = Mockito.mockingDetails(connection).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("findRealObjectsImpl")).toList();
        Assertions.assertEquals(1, lookups.size());
        Assertions.assertEquals(List.of("\"app.with.dot\"", "\"My Type\""), lookups.getFirst().getArgument(1));

        Mockito.clearInvocations(connection);
        String signature = "Tuple(amount UInt64, label String)";
        var fallback = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, syntaxNode("unknown", signature), signature);
        Assertions.assertEquals(signature, fallback.getDisplayName());
        Mockito.verify(connection).isDummy();
        Mockito.verify(connection).resolveDataType(monitor, signature);
        Mockito.verifyNoMoreInteractions(connection);

        Assertions.assertTrue(recognizedType("app.").nameParts().isEmpty());
        Assertions.assertTrue(recognizedType("numeric(10,").nameParts().isEmpty());
    }

    @Test
    void emptyTypeModifiersPreserveTypeIdentity() {
        SQLQueryExprType base = SQLQueryExprType.NUMERIC;
        Assertions.assertSame(base, SQLQueryExprType.withModifiers(base, List.of()));
        SQLQueryExprType modified = SQLQueryExprType.withModifiers(base, List.of("UNSIGNED"));
        Assertions.assertSame(modified, SQLQueryExprType.withModifiers(modified, List.of()));
    }

    @Test
    void typeModifiersPreserveOrderAndDuplicates() {
        SQLQueryExprType base = SQLQueryExprType.NUMERIC;
        var modifiers = new ArrayList<>(List.of("UNSIGNED", "ZEROFILL", "UNSIGNED"));
        var modified = SQLQueryExprType.withModifiers(base, modifiers);
        modifiers.clear();

        Assertions.assertEquals(List.of("UNSIGNED", "ZEROFILL", "UNSIGNED"), modified.getModifiers());
        Assertions.assertEquals("NUMERIC UNSIGNED ZEROFILL UNSIGNED", modified.getDisplayName());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> modified.getModifiers().add("OTHER"));
        Assertions.assertSame(modified, SQLQueryExprType.withModifiers(modified, modified.getModifiers()));

        var identical = SQLQueryExprType.withModifiers(base, List.of("UNSIGNED", "ZEROFILL", "UNSIGNED"));
        Assertions.assertEquals(modified.getModifiers(), SQLQueryExprType.tryCombineIfMatches(modified, identical).getModifiers());
        var reordered = SQLQueryExprType.withModifiers(base, List.of("UNSIGNED", "UNSIGNED", "ZEROFILL"));
        Assertions.assertSame(base, SQLQueryExprType.tryCombineIfMatches(modified, reordered));
        var deduplicated = SQLQueryExprType.withModifiers(base, List.of("UNSIGNED", "ZEROFILL"));
        Assertions.assertSame(base, SQLQueryExprType.tryCombineIfMatches(modified, deduplicated));
    }

    @Test
    void modifiedTypesDelegateMetadataAndMemberLookups() throws Exception {
        var monitor = new VoidProgressMonitor();
        var typedObject = Mockito.mock(DBSDataType.class);
        Mockito.when(typedObject.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(typedObject.getFullTypeName()).thenReturn("decimal");
        var base = SQLQueryExprType.forTypedObject(monitor, typedObject, SQLQuerySymbolClass.OBJECT);
        var modified = SQLQueryExprType.withModifiers(base, List.of("UNSIGNED"));
        Assertions.assertSame(typedObject, modified.getTypedDbObject());
        Assertions.assertSame(base.getDeclaratorDefinition(), modified.getDeclaratorDefinition());
        Assertions.assertEquals(DBPDataKind.NUMERIC, modified.getDataKind());
        Assertions.assertEquals("decimal UNSIGNED", modified.getDisplayName());

        var array = SQLQueryExprType.forSynthesizedArray("amounts", base.getDeclaratorDefinition(), base);
        var modifiedArray = SQLQueryExprType.withModifiers(array, List.of("UNSIGNED"));
        Assertions.assertSame(base, modifiedArray.findIndexedItemType(monitor, 1, null));

        var composite = SQLQueryExprType.forSynthesizedComposite(
            "record_type", Mockito.mock(DBPDataSource.class), base.getDeclaratorDefinition(), Map.of("amount", base));
        var modifiedComposite = SQLQueryExprType.withModifiers(composite, List.of("UNSIGNED"));
        Assertions.assertSame(base, modifiedComposite.findNamedMemberType(monitor, "amount"));
        Assertions.assertEquals(composite.getNamedMembers(monitor), modifiedComposite.getNamedMembers(monitor));
    }

    @Test
    void fullSignatureProviderResolvesNamedTupleMembers() throws Exception {
        var monitor = new VoidProgressMonitor();
        var dialect = BasicSQLDialect.INSTANCE;
        var source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBPDataTypeProvider.class, DBSObjectContainer.class));
        var provider = (DBPDataTypeProvider) source;
        var container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(source.getContainer()).thenReturn(container);
        Mockito.when(container.isExtraMetadataReadEnabled()).thenReturn(true);
        Mockito.when(((DBSObjectContainer) source).getDataSource()).thenReturn(source);
        Mockito.when(source.getSQLDialect()).thenReturn(dialect);
        var execution = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(execution.getDataSource()).thenReturn(source);
        var context = new SQLQueryRowsSourceContext(new SQLQueryConnectionRealContext(
            dialect, SQLQueryVariablesSubset.EMPTY, new SQLIdentifierDetector(dialect), execution,
            false, Map.of(), rows -> List.of()));
        var statistics = realMetadataContext(monitor, execution, dialect);

        String signature = "Tuple(amount UInt64, label String)";
        var amount = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(amount.getKind()).thenReturn(DBSTypeDescriptor.Kind.SIMPLE);
        Mockito.when(amount.getTypeName()).thenReturn("UInt64");
        var tupleDescriptor = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(tupleDescriptor.getKind()).thenReturn(DBSTypeDescriptor.Kind.COMPOSITE);
        Mockito.when(tupleDescriptor.getTypeName()).thenReturn(signature);
        Mockito.when(tupleDescriptor.findCompositeMember(monitor, "amount")).thenReturn(amount);
        var tuple = Mockito.mock(DBSDataType.class, Mockito.withSettings().extraInterfaces(DBSTypedObjectEx2.class));
        Mockito.when(tuple.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(tuple.getFullTypeName()).thenReturn(signature);
        Mockito.when(((DBSTypedObjectEx2) tuple).getTypeDescriptor(monitor)).thenReturn(tupleDescriptor);
        Mockito.when(provider.resolveDataType(monitor, signature)).thenReturn(tuple);

        SQLQueryExprType resolved = SQLQuerySemanticUtils.resolveTypeReference(
            context, statistics, typeReference(signature, "Tuple", true));
        Assertions.assertSame(tuple, resolved.getTypedDbObject());
        Assertions.assertEquals(DBPDataKind.STRUCT, resolved.getDataKind());
        Assertions.assertEquals("UInt64", resolved.findNamedMemberType(monitor, "amount").getDisplayName());
        Mockito.verify(provider).resolveDataType(monitor, signature);

        String qualifiedSignature = "app." + signature;
        Mockito.when(provider.resolveDataType(monitor, qualifiedSignature)).thenReturn(tuple);
        SQLQueryExprType qualified = SQLQuerySemanticUtils.resolveTypeReference(
            context, statistics, typeReference(qualifiedSignature, "app.Tuple", true));
        Assertions.assertSame(tuple, qualified.getTypedDbObject());
        Assertions.assertEquals("UInt64", qualified.findNamedMemberType(monitor, "amount").getDisplayName());

        String quotedSignature = "\"app\".\"Tuple\"(amount UInt64, label String)";
        Mockito.when(provider.resolveDataType(monitor, quotedSignature)).thenReturn(tuple);
        SQLQueryExprType quoted = SQLQuerySemanticUtils.resolveTypeReference(
            context, statistics, typeReference(quotedSignature, "\"app\".\"Tuple\"", true));
        Assertions.assertSame(tuple, quoted.getTypedDbObject());
        Assertions.assertEquals("UInt64", quoted.findNamedMemberType(monitor, "amount").getDisplayName());

        var bareTuple = Mockito.mock(DBSDataType.class);
        Mockito.when(bareTuple.getFullTypeName()).thenReturn("Tuple");
        Mockito.when(bareTuple.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(provider.resolveDataType(monitor, signature)).thenReturn(bareTuple);
        SQLQueryExprType fallback = SQLQuerySemanticUtils.resolveTypeReference(
            context, statistics, typeReference(signature, "Tuple", true));
        Assertions.assertSame(bareTuple, fallback.getTypedDbObject());
        Assertions.assertEquals("Tuple", fallback.getDisplayName());
        Assertions.assertNull(fallback.findNamedMemberType(monitor, "amount"));
        Mockito.when(provider.resolveDataType(monitor, qualifiedSignature)).thenReturn(bareTuple);
        SQLQueryExprType qualifiedFallback = SQLQuerySemanticUtils.resolveTypeReference(
            context, statistics, typeReference(qualifiedSignature, "app.Tuple", true));
        Assertions.assertSame(bareTuple, qualifiedFallback.getTypedDbObject());
        Assertions.assertEquals("Tuple", qualifiedFallback.getDisplayName());
    }

    @Test
    void precisionFallsBackToBaseNameMetadata() throws Exception {
        var monitor = new VoidProgressMonitor();
        var dialect = BasicSQLDialect.INSTANCE;
        var source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBPDataTypeProvider.class, DBSObjectContainer.class));
        var provider = (DBPDataTypeProvider) source;
        var container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(source.getContainer()).thenReturn(container);
        Mockito.when(container.isExtraMetadataReadEnabled()).thenReturn(true);
        Mockito.when(((DBSObjectContainer) source).getDataSource()).thenReturn(source);
        Mockito.when(source.getSQLDialect()).thenReturn(dialect);
        var numeric = Mockito.mock(DBSDataType.class);
        Mockito.when(numeric.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(numeric.getFullTypeName()).thenReturn("numeric");
        var schema = Mockito.mock(DBSSchema.class);
        Mockito.when(schema.getChild(monitor, "NUMERIC")).thenReturn(numeric);
        var defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(defaults.getDefaultSchema()).thenReturn(schema);
        var execution = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(execution.getDataSource()).thenReturn(source);
        Mockito.when(execution.getContextDefaults()).thenReturn(defaults);
        var context = new SQLQueryRowsSourceContext(new SQLQueryConnectionRealContext(
            dialect, SQLQueryVariablesSubset.EMPTY, new SQLIdentifierDetector(dialect), execution,
            false, Map.of(), rows -> List.of()));

        SQLQueryExprType resolved = SQLQuerySemanticUtils.resolveTypeReference(
            context, realMetadataContext(monitor, execution, dialect), typeReference("numeric(10,2)", null, true));
        Assertions.assertSame(numeric, resolved.getTypedDbObject());
        Assertions.assertEquals(DBPDataKind.NUMERIC, resolved.getDataKind());
        Mockito.verify(provider).resolveDataType(monitor, "numeric(10,2)");
        Mockito.verify(provider).resolveDataType(monitor, "numeric");
        Mockito.verify(schema).getChild(monitor, "NUMERIC");
    }

    @Test
    void providerFullNameWithModifiersDoesNotRepeatSuffix() throws Exception {
        var monitor = new VoidProgressMonitor();
        var dialect = Mockito.spy(BasicSQLDialect.INSTANCE);
        var features = new SQLDialectFeatures() {
            @Override
            public Set<String> getTypeModifierKeywords() {
                return Set.of("UNSIGNED", "ZEROFILL");
            }
        };
        Mockito.doReturn(features).when(dialect).getDialectFeatures();
        var source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBPDataTypeProvider.class));
        var provider = (DBPDataTypeProvider) source;
        var execution = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(execution.getDataSource()).thenReturn(source);
        var context = new SQLQueryRowsSourceContext(new SQLQueryConnectionRealContext(
            dialect, SQLQueryVariablesSubset.EMPTY, new SQLIdentifierDetector(dialect), execution,
            false, Map.of(), rows -> List.of()));
        var metadata = Mockito.mock(DBSDataType.class);
        Mockito.when(metadata.getFullTypeName()).thenReturn("int UNSIGNED ZEROFILL");
        Mockito.when(metadata.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(provider.resolveDataType(monitor, "int UNSIGNED ZEROFILL")).thenReturn(metadata);

        SQLQueryExprType resolved = SQLQuerySemanticUtils.resolveTypeReference(
            context, realMetadataContext(monitor, execution, dialect),
            typeReference("int", null, false, "UNSIGNED", "ZEROFILL"));
        Assertions.assertSame(metadata, resolved.getTypedDbObject());
        Assertions.assertEquals(List.of("UNSIGNED", "ZEROFILL"), resolved.getModifiers());
        Assertions.assertEquals("int UNSIGNED ZEROFILL", resolved.getDisplayName());
        Mockito.verify(provider).resolveDataType(monitor, "int UNSIGNED ZEROFILL");

        var unsigned = Mockito.mock(DBSDataType.class);
        Mockito.when(unsigned.getFullTypeName()).thenReturn("int UNSIGNED");
        Mockito.when(unsigned.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(provider.resolveDataType(monitor, "int UNSIGNED")).thenReturn(unsigned);
        SQLQueryExprType singleModifier = SQLQuerySemanticUtils.resolveTypeReference(
            context, realMetadataContext(monitor, execution, dialect), typeReference("int", null, false, "UNSIGNED"));
        Assertions.assertSame(unsigned, singleModifier.getTypedDbObject());
        Assertions.assertEquals(List.of("UNSIGNED"), singleModifier.getModifiers());
        Assertions.assertEquals("int UNSIGNED", singleModifier.getDisplayName());
    }

    private static SQLQueryRecognitionContext realMetadataContext(
        @NotNull VoidProgressMonitor monitor, @NotNull DBCExecutionContext execution, @NotNull SQLDialect dialect
    ) {
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        return new SQLQueryRecognitionContext(monitor, execution, true, false, syntax, dialect);
    }

    private static SQLQueryModelRecognizer.TypeReference typeReference(
        @NotNull String spelling, String qualifiedName, boolean hasArguments, String... modifiers
    ) {
        List<STMTreeNode> typeChildren = new ArrayList<>();
        String name = qualifiedName != null ? qualifiedName : hasArguments ? spelling.substring(0, spelling.indexOf('(')) : spelling;
        List<STMTreeNode> identifiers = new ArrayList<>();
        for (String part : SQLUtils.splitFullIdentifier(name, ".", BasicSQLDialect.INSTANCE.getIdentifierQuoteStrings(), true)) {
            if (!identifiers.isEmpty()) {
                identifiers.add(syntaxNode("Period", "."));
            }
            identifiers.add(syntaxNode(STMKnownRuleNames.identifier, part));
        }
        typeChildren.add(syntaxNode(STMKnownRuleNames.qualifiedName, name, identifiers));
        if (hasArguments) {
            typeChildren.add(syntaxNode(STMKnownRuleNames.typeArguments, spelling.substring(spelling.indexOf('('))));
        }
        STMTreeNode dataType = syntaxNode(STMKnownRuleNames.dataType, spelling, typeChildren);
        List<STMTreeNode> children = new ArrayList<>();
        children.add(dataType);
        for (String modifier : modifiers) {
            children.add(syntaxNode(STMKnownRuleNames.typeModifier, modifier));
        }
        return new SQLQueryModelRecognizer.TypeReference(spelling, name,
            Arrays.stream(SQLUtils.splitFullIdentifier(name, ".", BasicSQLDialect.INSTANCE.getIdentifierQuoteStrings(), true))
                .map(part -> SQLUtils.identifierToCanonicalForm(BasicSQLDialect.INSTANCE, part, false, false)).toList(),
            List.of(modifiers), syntaxNode("typeReference", spelling, children), null);
    }

    private static SQLQueryModelRecognizer.TypeReference recognizedType(String spelling) throws Exception {
        String sql = "SELECT CAST(1 AS " + spelling + ")";
        var model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        var cast = model.findNodeContaining(sql.indexOf("CAST") + 1);
        var field = org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTypeCastExpression.class
            .getDeclaredField("typeReference");
        field.setAccessible(true);
        return (SQLQueryModelRecognizer.TypeReference) field.get(cast);
    }

    @Test
    void recognizedTypeBindsProviderMetadataWithoutParentAndSurvivesDescriptorFailure() throws Exception {
        var monitor = new VoidProgressMonitor();
        var connection = Mockito.mock(SQLQueryConnectionContext.class);
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(statistics.getMonitor()).thenReturn(monitor);
        var metadata = Mockito.mock(DBSDataType.class, Mockito.withSettings().extraInterfaces(DBSTypedObjectEx2.class));
        Mockito.when(metadata.getFullTypeName()).thenReturn("amount");
        Mockito.when(metadata.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(connection.resolveDataType(monitor, "app.amount")).thenReturn(metadata);
        var reference = recognizedType("app.amount");
        var terminal = reference.qualifiedName().parts.getLast();
        Assertions.assertSame(metadata, SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference).getTypedDbObject());
        var definition = terminal.getDefinition();
        Assertions.assertNotNull(definition);
        Assertions.assertEquals(SQLQuerySymbolClass.TYPE, terminal.getSymbolClass());
        Assertions.assertEquals(SQLQuerySymbolClass.TYPE, definition.getSymbolClass());
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.DbObjectRef.class, terminal.getOrigin());
        SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
        Assertions.assertSame(definition, terminal.getDefinition());

        Mockito.when(metadata.getComponentType(monitor)).thenThrow(new DBException("descriptor unavailable"));
        var failed = recognizedType("app.amount");
        Assertions.assertEquals("app.amount", SQLQuerySemanticUtils.resolveTypeReference(context, statistics, failed).getDisplayName());
        Assertions.assertNotNull(failed.qualifiedName().parts.getLast().getDefinition());
    }

    @Test
    void unresolvedTypeBindsOnlySchemaPrefix() throws Exception {
        var monitor = new VoidProgressMonitor();
        var schema = Mockito.mock(DBSSchema.class);
        var connection = Mockito.mock(SQLQueryConnectionContext.class, invocation ->
            invocation.getMethod().getName().equals("findRealObjectsImpl") && invocation.getArgument(1).equals(List.of("APP"))
                ? List.of(schema) : Mockito.RETURNS_DEFAULTS.answer(invocation));
        var context = Mockito.mock(SQLQueryRowsSourceContext.class);
        Mockito.when(context.getConnectionInfo()).thenReturn(connection);
        var statistics = Mockito.mock(SQLQueryRecognitionContext.class);
        Mockito.when(statistics.useRealMetadata()).thenReturn(true);
        Mockito.when(statistics.getMonitor()).thenReturn(monitor);
        for (String spelling : List.of("app.missing", "app.")) {
            var reference = recognizedType(spelling);
            var result = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
            Assertions.assertEquals(spelling, result.getDisplayName());
            Assertions.assertNull(result.getTypedDbObject());
            var prefix = reference.qualifiedName().parts.getFirst();
            Assertions.assertNotNull(prefix.getDefinition());
            Assertions.assertEquals(SQLQuerySymbolClass.SCHEMA, prefix.getSymbolClass());
            var definition = prefix.getDefinition();
            SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
            Assertions.assertSame(definition, prefix.getDefinition());
            if (reference.qualifiedName().parts.getLast() != null) {
                Assertions.assertNull(reference.qualifiedName().parts.getLast().getDefinition());
                Assertions.assertEquals(SQLQuerySymbolClass.TYPE, reference.qualifiedName().parts.getLast().getSymbolClass());
                var origin = Assertions.assertInstanceOf(SQLQuerySymbolOrigin.DbObjectFromDbObject.class,
                    reference.qualifiedName().parts.getLast().getOrigin());
                Assertions.assertEquals(SQLQuerySymbolOrigin.DbObjectFilterMode.TYPE, origin.getFilterMode());
            }
        }
    }

    @Test
    void sharedDeclarationRegistersTypeNameOnlyOnce() throws Exception {
        String sql = "BEGIN DECLARE a, b app.amount; END";
        var model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        Assertions.assertEquals(1, model.getAllSymbols().stream().filter(s -> s.getName().equals("APP")).count());
        Assertions.assertEquals(1, model.getAllSymbols().stream().filter(s -> s.getName().equals("AMOUNT")).count());
        var declaration = Assertions.assertInstanceOf(SQLQueryVariableDeclarationStatement.class,
            model.findNodeContaining(sql.indexOf("DECLARE") + 1));
        Assertions.assertEquals(2, declaration.getDeclarations().size());
        var field = SQLQueryVariableDeclarationStatement.Declaration.class.getDeclaredField("typeReference");
        field.setAccessible(true);
        Assertions.assertSame(field.get(declaration.getDeclarations().getFirst()), field.get(declaration.getDeclarations().getLast()));
    }

    private static STMTreeNode syntaxNode(@NotNull String name, @NotNull String text) {
        return syntaxNode(name, text, List.of());
    }

    private static STMTreeNode syntaxNode(@NotNull String name, @NotNull String text, @NotNull List<STMTreeNode> children) {
        STMTreeNode node = Mockito.mock(STMTreeNode.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(node.getNodeName()).thenReturn(name);
        Mockito.when(node.getTextContent()).thenReturn(text);
        Mockito.when(node.getChildCount()).thenReturn(children.size());
        for (int i = 0; i < children.size(); i++) {
            Mockito.doReturn(children.get(i)).when(node).getChildNode(i);
        }
        return node;
    }

    private static final String STMT_SELECT =
        "SELECT t.id, t.amount FROM test_table t JOIN parent_table p ON p.id = t.parent_id " +
            "WHERE t.amount > 0 GROUP BY t.id, t.amount HAVING COUNT(*) > 0 ORDER BY t.id";

    private static final String STMT_RECURSIVE_CTE =
        "WITH RECURSIVE hierarchy (id, parent_id, depth, total_amount) AS (" +
            "  SELECT id, parent_id, 0, amount FROM test_table WHERE parent_id IS NULL " +
            "  UNION ALL " +
            "  SELECT child.id, child.parent_id, parent.depth + 1, parent.total_amount + child.amount " +
            "  FROM test_table child JOIN hierarchy parent ON child.parent_id = parent.id " +
            "  WHERE parent.depth < 10" +
            "), filtered AS (" +
            "  SELECT id, parent_id, depth, total_amount FROM hierarchy WHERE total_amount > 100" +
            ") " +
            "SELECT f.id, f.depth, f.total_amount FROM filtered f " +
            "JOIN parent_table p ON p.id = f.parent_id WHERE f.depth > 0 ORDER BY f.depth, f.id";

    private static final String STMT_INSERT = "INSERT INTO test_table (id, amount) VALUES (1, 10), (2, 20)";

    private static final String STMT_UPDATE =
        "UPDATE test_table SET amount = amount + 1 WHERE id IN (SELECT id FROM parent_table)";

    private static final String STMT_DELETE = "DELETE FROM test_table WHERE id = 1";

    private static final String STMT_CREATE_TABLE =
        "CREATE TABLE test_table (id INTEGER PRIMARY KEY, parent_id INTEGER, " +
            "CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_table (id), CHECK (id > 0))";

    private static final String STMT_ALTER_TABLE = "ALTER TABLE test_table ADD COLUMN amount INTEGER DEFAULT 0";

    private static final String STMT_DROP_TABLE = "DROP TABLE IF EXISTS test_table";

    private static final String STMT_CALL = "CALL test_proc(1, 'value')";

    private static final String STMT_CREATE_VIEW = "CREATE VIEW test_view AS SELECT id FROM test_table";

    private static final String STMT_CREATE_SCHEMA = "CREATE SCHEMA test_schema";

    private static final String STMT_DROP_SCHEMA = "DROP SCHEMA test_schema CASCADE";

    private static final String STMT_DECLARE_AND_SELECT = """
        DECLARE @start_date DATE = DATEADD(MONTH, -1, CURRENT_TIMESTAMP)
        DECLARE @end_date DATE = CURRENT_TIMESTAMP
        SELECT
        e.first_name AS FirstName
        , e.last_name AS LastName
        , e.hire_date AS HireDate
        FROM employee_table e
        WHERE e.hire_date BETWEEN @start_date AND @end_date
        """;

    @Test
    public void recognizesConsecutiveDeclarationsAndSelectAsOneQuery() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(STMT_DECLARE_AND_SELECT);

        Assertions.assertEquals(2, scope.getVariableClauses().size());
        Assertions.assertEquals(
            List.of("@start_date", "@end_date"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(
            2, scope.getVariableClauses().stream()
                .map(SQLQueryVariableClause::getValueExpression).filter(value -> value != null).count()
        );
        Assertions.assertNotNull(scope.getBody());
        Assertions.assertNotNull(scope.getResultingVariables());
    }

    @Test
    public void recognizesStandaloneVariableStatements() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(
            "DECLARE @first INT = 1, @second VARCHAR(20) DEFAULT 'x'"
        );

        Assertions.assertNull(scope.getBody());
        Assertions.assertEquals(
            List.of("@first", "@second"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(
            List.of(
                SQLQueryVariableInfo.OperationKind.DECLARATION,
                SQLQueryVariableInfo.OperationKind.DECLARATION
            ),
            scope.getVariableClauses().stream().map(SQLQueryVariableClause::getKind).toList()
        );
        Assertions.assertEquals(
            2, scope.getVariableClauses().stream()
                .map(SQLQueryVariableClause::getValueExpression).filter(value -> value != null).count()
        );

        SQLQueryVariableStatementModel assignment = recognizeVariableScope("SET @first = 2");
        Assertions.assertEquals(
            List.of(SQLQueryVariableInfo.OperationKind.ASSIGNMENT),
            assignment.getVariableClauses().stream().map(SQLQueryVariableClause::getKind).toList()
        );
    }

    @Test
    public void variableAssignmentsDoNotStealExistingSetStatements() {
        for (String sql : List.of("SET TRANSACTION READ ONLY", "SET SCHEMA test_schema")) {
            SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
            Assertions.assertNotNull(model);
            STMTreeNode queryNode = model.getSyntaxNode().findFirstNonErrorChild();
            Assertions.assertNotNull(queryNode);
            Assertions.assertEquals(STMKnownRuleNames.sqlQueryBody, queryNode.getNodeName());
        }
    }

    @Test
    public void variableStatementRecognitionSkipsOtherStatements() {
        for (String sql : List.of(
            "SET TRANSACTION READ ONLY",
            "SET SCHEMA test_schema",
            "UPDATE test_table SET amount = 1"
        )) {
            Assertions.assertNull(SQLQueryModelRecognizer.recognizeVariableStatement(
                createContext(new VoidProgressMonitor()), sql, SQLQueryVariablesSubset.EMPTY));
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeVariableStatement(
            createContext(new VoidProgressMonitor()), "SET @value = 1", SQLQueryVariablesSubset.EMPTY);
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());
    }

    @Test
    public void variableStatementRecognitionSkipsRegularQueryBody() {
        SQLQueryVariableStatementModel regularModel = recognizeVariableScope(STMT_DECLARE_AND_SELECT);
        SQLQueryModel trackingModel = SQLQueryModelRecognizer.recognizeVariableStatement(
            createContext(new VoidProgressMonitor()),
            STMT_DECLARE_AND_SELECT,
            SQLQueryVariablesSubset.EMPTY
        );
        Assertions.assertNotNull(trackingModel);
        SQLQueryVariableStatementModel trackingStatement = Assertions.assertInstanceOf(
            SQLQueryVariableStatementModel.class,
            trackingModel.getQueryModel()
        );

        Assertions.assertNotNull(regularModel.getBody());
        Assertions.assertNull(trackingStatement.getBody());
        SQLQueryVariablesSubset regularVariables = Objects.requireNonNull(regularModel.getResultingVariables());
        SQLQueryVariablesSubset trackingVariables = Objects.requireNonNull(trackingStatement.getResultingVariables());
        Assertions.assertEquals(
            regularVariables.getVariables().stream().map(SQLQueryVariableInfo::rawName).toList(),
            trackingVariables.getVariables().stream().map(SQLQueryVariableInfo::rawName).toList()
        );
        Assertions.assertEquals(
            regularVariables.getVariables().stream().map(variable -> variable.type().getDisplayName()).toList(),
            trackingVariables.getVariables().stream().map(variable -> variable.type().getDisplayName()).toList()
        );
    }

    @Test
    public void resolvesVisibleVariableReferences() {
        SQLQueryVariableInfo definition = new SQLQueryVariableInfo(
            "@value",
            "value",
            SQLQueryExprType.forExplicitTypeRef("INT"),
            SQLScriptVariableScope.BATCH,
            SQLQueryVariableInfo.OperationKind.DECLARATION,
            0
        );

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "SELECT @VALUE",
            SQLQueryVariablesSubset.makeSnapshot(
                SQLScriptVariableScope.BATCH,
                Map.of(definition.canonicalName(), definition)
            )
        );

        Assertions.assertNotNull(model);
        SQLQuerySymbolEntry variable = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("@VALUE"))
            .findFirst()
            .orElseThrow();
        Assertions.assertSame(definition, variable.getDefinition());
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.ScriptVariableRef.class, variable.getOrigin());
    }

    @Test
    public void keepsClientVariableOriginsClientSpecific() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "SELECT ${value}, :parameter"
        );

        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> clientVariables = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("${value}") || symbol.getRawName().equals(":parameter"))
            .toList();
        Assertions.assertEquals(2, clientVariables.size());
        Assertions.assertTrue(clientVariables.stream()
            .noneMatch(symbol -> symbol.getOrigin() instanceof SQLQuerySymbolOrigin.ScriptVariableRef));
    }

    @Test
    public void resolvesVisibleUnprefixedVariableReferences() {
        SQLQueryVariableInfo definition = new SQLQueryVariableInfo(
            "current_value",
            "current_value",
            SQLQueryExprType.forExplicitTypeRef("INT"),
            SQLScriptVariableScope.SESSION,
            SQLQueryVariableInfo.OperationKind.DECLARATION,
            0
        );
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context,
            "SELECT current_value",
            SQLQueryVariablesSubset.makeSnapshot(
                SQLScriptVariableScope.SESSION,
                Map.of(definition.canonicalName(), definition)
            )
        );

        Assertions.assertNotNull(model);
        Assertions.assertTrue(context.getProblems().isEmpty());
        SQLQuerySymbolEntry variable = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("current_value"))
            .findFirst()
            .orElseThrow();
        Assertions.assertSame(definition, variable.getDefinition());
        Assertions.assertFalse(variable.getOrigin() instanceof SQLQuerySymbolOrigin.ScriptVariableRef);
    }

    @Test
    public void recognizesUnprefixedSessionVariables() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(
            "DECLARE OR REPLACE VARIABLE current_value INT DEFAULT 1");

        Assertions.assertEquals(
            List.of("current_value"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(1, scope.getVariableClauses().size());

        SQLQueryVariableStatementModel assignment = recognizeVariableScope("SET VARIABLE current_value = 2");
        Assertions.assertEquals(
            List.of("current_value"),
            assignment.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
    }

    @Test
    public void recognizesDatabricksVarAssignment() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope("SET VAR current_value = 2");

        Assertions.assertEquals(
            List.of("current_value"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
    }

    @Test
    public void incompleteVariableDeclarationDoesNotFailRecognition() {
        Assertions.assertDoesNotThrow(() ->
            SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), "DECLARE \"")
        );
    }

    @Test
    public void selectCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_SELECT);
    }

    @Test
    public void recursiveCteCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_RECURSIVE_CTE);
    }

    @Test
    public void insertCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_INSERT);
    }

    @Test
    public void updateCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_UPDATE);
    }

    @Test
    public void deleteCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_DELETE);
    }

    @Test
    public void createTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_CREATE_TABLE);
    }

    @Test
    public void alterTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_ALTER_TABLE);
    }

    @Test
    public void dropTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_DROP_TABLE);
    }

    @Test
    public void callCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_CALL);
    }

    @Test
    public void unsupportedCreateViewCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_CREATE_VIEW);
    }

    @Test
    public void unsupportedCreateSchemaCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_CREATE_SCHEMA);
    }

    @Test
    public void unsupportedDropSchemaCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_DROP_SCHEMA);
    }

    @Test
    public void resolvesSqlWithinProceduralBlock() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN SELECT id FROM tab; END"
        );
        Assertions.assertNotNull(model);
        SQLQueryRoutineStatementModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("tab")));
    }

    @Test
    public void declaredLocalTableSuppliesColumnsToNestedSql() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN DECLARE @items TABLE (id int); SELECT id FROM @items; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("@items")));
    }

    @Test
    public void functionParameterIsVisibleInItsSqlBody() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE FUNCTION f(x int) RETURNS int AS $body$ BEGIN SELECT x; END; $body$ LANGUAGE sql"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().filter(s -> s.getRawName().equals("x"))
            .anyMatch(s -> s.getDefinition() != null));
    }

    @Test
    public void routineResolvesUnnamedArgumentsAndScalarReturn() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()),
            "CREATE FUNCTION f(int, named varchar, numeric) RETURNS integer AS BEGIN SELECT named; END");
        Assertions.assertNotNull(model);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertEquals(3, routine.getArguments().size());
        Assertions.assertNull(routine.getArguments().getFirst().declaration());
        Assertions.assertNotNull(routine.getArguments().get(1).declaration());
        Assertions.assertNull(routine.getArguments().getLast().declaration());
        Assertions.assertEquals(List.of("int", "varchar", "numeric"), routine.getArguments().stream()
            .map(argument -> argument.resolvedType().getDisplayName()).toList());
        Assertions.assertEquals("varchar", routine.getArguments().get(1).declaration()
            .getDeclarations().getFirst().getType().getDisplayName());
        Assertions.assertEquals(List.of("integer"), routine.getReturnTypes().stream()
            .map(SQLQueryExprType::getDisplayName).toList());
        SQLQueryRoutineReturn.Scalar scalar = Assertions.assertInstanceOf(
            SQLQueryRoutineReturn.Scalar.class, routine.getReturnSpec());
        Assertions.assertEquals("integer", scalar.resolvedType().getDisplayName());
        Assertions.assertTrue(routine.getReturnTableTypes().isEmpty());
    }

    @Test
    public void routineResolvesTableReturnTypesWithoutCreatingInputParameters() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()),
            "CREATE FUNCTION f(x int) RETURNS TABLE (output_value varchar, int) AS BEGIN SELECT x; END");
        Assertions.assertNotNull(model);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertEquals(1, routine.getArguments().size());
        Assertions.assertTrue(routine.getReturnTypes().isEmpty());
        Assertions.assertEquals(List.of("varchar", "int"), routine.getReturnTableTypes().stream()
            .map(SQLQueryExprType::getDisplayName).toList());
        SQLQueryRoutineReturn.TableColumns table = Assertions.assertInstanceOf(
            SQLQueryRoutineReturn.TableColumns.class, routine.getReturnSpec());
        Assertions.assertEquals(2, table.fields().size());
        Assertions.assertEquals("output_value", table.fields().getFirst().name().getRawName());
        Assertions.assertEquals("varchar", table.fields().getFirst().resolvedType().getDisplayName());
        Assertions.assertNull(table.fields().getFirst().name().getDefinition());
    }

    @Test
    public void routineResolvesNamedOutputListAndExposesOutputInBody() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()),
            "CREATE PROCEDURE p(input_value int) RETURNS (result varchar) AS BEGIN SELECT result; END");
        Assertions.assertNotNull(model);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertEquals(1, routine.getArguments().size());
        Assertions.assertEquals(List.of("varchar"), routine.getReturnTableTypes().stream()
            .map(SQLQueryExprType::getDisplayName).toList());
        SQLQueryRoutineReturn.OutputParameters output = Assertions.assertInstanceOf(
            SQLQueryRoutineReturn.OutputParameters.class, routine.getReturnSpec());
        Assertions.assertEquals("varchar", output.fields().getFirst().resolvedType().getDisplayName());
        Assertions.assertNotNull(output.fields().getFirst().name().getDefinition());
        Assertions.assertEquals(1, model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equalsIgnoreCase("varchar")).count());
        Assertions.assertTrue(model.getAllSymbols().stream().filter(s -> s.getRawName().equals("result"))
            .anyMatch(s -> s.getDefinition() != null));
    }

    @Test
    public void routineReturnDistinguishesAbsentAndIncompleteDeclarations() {
        SQLQueryModel absentModel = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()),
            "CREATE PROCEDURE p() AS BEGIN SELECT 1; END");
        Assertions.assertNotNull(absentModel);
        SQLQueryRoutineCreateModel absent = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class,
            absentModel.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryRoutineReturn.None.class, absent.getReturnSpec());

        SQLQueryModel incompleteModel = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()),
            "CREATE FUNCTION f() RETURNS");
        Assertions.assertNotNull(incompleteModel);
        SQLQueryRoutineCreateModel incomplete = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class,
            incompleteModel.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryRoutineReturn.Incomplete.class, incomplete.getReturnSpec());
    }

    @Test
    public void parenthesizedTableFunctionBodyRetainsNestedSqlAndParameterContext() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "CREATE TABLE FUNCTION dataset.qa_table_fn(x INT64) AS (SELECT x FROM rects3rects)"
        );
        Assertions.assertNotNull(model);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        SQLQueryEmbeddedStatement nestedSql = Assertions.assertInstanceOf(
            SQLQueryEmbeddedStatement.class, body.getStatements().getFirst()
        );
        Assertions.assertNotNull(nestedSql.getSql());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("rects3rects")));
        List<SQLQuerySymbolEntry> parameter = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(2, parameter.size());
        Assertions.assertSame(parameter.getFirst(), parameter.getLast().getDefinition());
    }

    @Test
    public void proceduralCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint("CREATE PROCEDURE p AS BEGIN DECLARE v int; SELECT v; END");
    }

    @Test
    public void triggerTransitionRowsAreVisibleInsideBody() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE TRIGGER tr AFTER UPDATE ON tab REFERENCING NEW AS n FOR EACH ROW " +
                "BEGIN SELECT n.id; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryTriggerCreateModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("n")));
    }

    @Test
    public void declaredCursorIsLinkedFromProceduralStatements() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
                "BEGIN OPEN c; FETCH NEXT FROM c INTO x; CLOSE c; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().filter(s -> s.getRawName().equals("c"))
            .anyMatch(s -> s.getDefinition() != null));
    }

    @Test
    public void temporaryTableColumnsResolveWithinProcedure() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (id int); " +
                "INSERT INTO scratch (id) VALUES (1); SELECT id FROM scratch; DROP TABLE scratch; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("scratch")));
    }

    @Test
    public void prefixedTemporaryTableColumnsResolveWithinProcedure() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN CREATE TABLE #scratch (id int); SELECT id FROM #scratch; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("#scratch")));
    }

    @Test
    public void droppingMultipleTemporaryRelationsPreservesOtherRelations() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "SELECT 1"
        );
        Assertions.assertNotNull(model);
        STMTreeNode node = model.getSyntaxNode();
        SQLQueryRowsSourceContext original = new SQLQueryRowsSourceContext(new SQLQueryConnectionDummyContext(
            BasicSQLDialect.INSTANCE, SQLQueryVariablesSubset.EMPTY, Set.of(), Set.of()
        ));
        Assertions.assertSame(original, original.withoutTemporaryRelations(List.of()));
        Assertions.assertSame(original, original.withoutTemporaryRelations(List.of("missing")));

        SQLQueryRowsSourceContext current = original;
        for (String name : List.of("first", "second", "remaining")) {
            SQLQuerySymbolEntry declaration = new SQLQuerySymbolEntry(node, name, name, null);
            SQLQueryRowsLocalTableModel rows = new SQLQueryRowsLocalTableModel(node, declaration, List.of());
            current = current.withTemporaryRelation(new SQLQueryLocalVariableInfo(
                declaration, SQLDialectFeatures.LocalVariableKind.ROWSET, SQLQueryExprType.UNKNOWN, rows, false));
        }
        SQLQueryRowsSourceContext afterDrop = current.withoutTemporaryRelations(List.of("first", "missing", "first", "second"));
        Assertions.assertSame(afterDrop, afterDrop.withoutTemporaryRelations(List.of("missing", "second")));
        for (String name : List.of("first", "second")) {
            Assertions.assertNull(afterDrop.findDynamicRowsSource(new SQLQuerySymbolEntry(node, name, name, null)));
        }
        Assertions.assertNotNull(afterDrop.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "remaining", "remaining", null)));
        SQLQueryRowsSourceContext outer = current.withTemporaryRelationsFrom(afterDrop);
        Assertions.assertNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "first", "first", null)));
        Assertions.assertNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "second", "second", null)));
        Assertions.assertNotNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "remaining", "remaining", null)));
    }

    @Test
    public void dropTableUsesCollectedUnqualifiedNamesForTemporaryRelations() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE first (id int); " +
            "CREATE TEMP TABLE second (id int); CREATE TEMP TABLE remaining (id int); " +
            "DROP TABLE first, second, missing, schema.remaining; " +
            "SELECT id FROM first; SELECT id FROM second; SELECT id FROM remaining; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> remaining = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("remaining"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, remaining.size());
        Assertions.assertSame(remaining.getFirst(), remaining.getLast().getDefinition());
        for (String name : List.of("first", "second")) {
            List<SQLQuerySymbolEntry> symbols = model.getAllSymbols().stream()
                .filter(s -> s.getRawName().equals(name))
                .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
            Assertions.assertEquals(3, symbols.size());
            Assertions.assertNotSame(symbols.getFirst(), symbols.getLast().getDefinition());
        }
    }

    @Test
    public void nestedBlockRetainsItsFinalTemporaryRelationSnapshot() {
        String sql = "CREATE PROCEDURE p AS BEGIN " +
            "CREATE TEMP TABLE removed (id int); CREATE TEMP TABLE replaced (id int); " +
            "CREATE TEMP TABLE retained (id int); " +
            "BEGIN DROP TABLE removed, replaced; CREATE TEMP TABLE replaced (id text); END; " +
            "SELECT id FROM removed; SELECT id FROM replaced; SELECT id FROM retained; END";
        SQLQueryRecognitionContext recognitionContext = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(recognitionContext, sql);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(recognitionContext.getProblems().stream().noneMatch(p ->
            p.getMessage().contains("replaced") || p.getMessage().contains("retained")), recognitionContext.getProblems().toString());
        int trailingSelect = sql.lastIndexOf("SELECT id FROM replaced");
        Assertions.assertTrue(recognitionContext.getProblems().stream().noneMatch(p ->
            p.getInterval().a >= trailingSelect), recognitionContext.getProblems().toString());

        List<SQLQuerySymbolEntry> removed = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("removed"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, removed.size());
        Assertions.assertNotSame(removed.getFirst(), removed.getLast().getDefinition());

        List<SQLQuerySymbolEntry> replaced = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("replaced"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(4, replaced.size());
        Assertions.assertSame(replaced.get(2), replaced.getLast().getDefinition());

        List<SQLQuerySymbolEntry> retained = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("retained"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(2, retained.size());
        Assertions.assertSame(retained.getFirst(), retained.getLast().getDefinition());
    }

    @Test
    public void nestedBlockVariableShadowsOuterDeclarationOnlyInsideItsBlock() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "CREATE PROCEDURE p AS DECLARE x int; BEGIN " +
                "DECLARE x text; BEGIN SELECT x; END; SELECT x; END"
        );
        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(4, names.size());
        Assertions.assertSame(names.get(1), names.get(2).getDefinition());
        Assertions.assertSame(names.get(0), names.get(3).getDefinition());
    }

    @Test
    public void programmaticalDefinitionsDistinguishBodyCapabilityFromBodyPresence() {
        SQLQueryModel incomplete = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "CREATE PROCEDURE p"
        );
        Assertions.assertNotNull(incomplete);
        SQLQueryProgrammaticalBodyStatementModel routine = Assertions.assertInstanceOf(
            SQLQueryProgrammaticalBodyStatementModel.class, incomplete.getQueryModel()
        );
        Assertions.assertNull(routine.getBody());
        Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, routine);

        SQLQueryModel drop = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "DROP PROCEDURE p"
        );
        Assertions.assertNotNull(drop);
        Assertions.assertInstanceOf(SQLQueryRoutineDropModel.class, drop.getQueryModel());

        SQLQueryModel alter = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "ALTER FUNCTION p"
        );
        Assertions.assertNotNull(alter);
        Assertions.assertInstanceOf(SQLQueryRoutineAlterModel.class, alter.getQueryModel());
    }

    @Test
    public void assignmentInsideRoutineHasStatementModelAndResolvesLocalName() {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; SELECT x; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryAssignmentStatement.class, model.findNodeContaining(sql.indexOf("SET x") + 1));
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, names.size());
        Assertions.assertSame(names.get(0), names.get(1).getDefinition());
        Assertions.assertSame(names.get(0), names.get(2).getDefinition());
    }

    @Test
    public void anonymousBlockAndBodyFreeTriggerHaveDistinctModelFamilies() {
        SQLQueryModel block = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "BEGIN SELECT 1; END"
        );
        Assertions.assertNotNull(block);
        SQLQueryAnonymousBlockModel anonymous = Assertions.assertInstanceOf(SQLQueryAnonymousBlockModel.class, block.getQueryModel());
        Assertions.assertNotNull(anonymous.getBody());

        SQLQueryModel drop = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "DROP TRIGGER t ON tab"
        );
        Assertions.assertNotNull(drop);
        Assertions.assertInstanceOf(SQLQueryTriggerDropModel.class, drop.getQueryModel());

        SQLQueryModel alter = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "ALTER TRIGGER t"
        );
        Assertions.assertNotNull(alter);
        Assertions.assertInstanceOf(SQLQueryTriggerAlterModel.class, alter.getQueryModel());

        SQLQueryModel cursor = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "OPEN c"
        );
        Assertions.assertNotNull(cursor);
        Assertions.assertInstanceOf(SQLQueryCursorOperationStatement.class, cursor.getQueryModel());
    }

    @Test
    public void branchesHaveIndependentLocalVariables() {
        String sql = "CREATE PROCEDURE p AS BEGIN IF 1 = 1 THEN DECLARE x int; SELECT x; " +
            "ELSE DECLARE x text; SELECT x; END IF; SELECT x; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        SQLQueryConditionalStatement branch = Assertions.assertInstanceOf(
            SQLQueryConditionalStatement.class, model.findNodeContaining(sql.indexOf("IF 1") + 1)
        );
        Assertions.assertEquals(2, branch.getBranches().size());
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(5, names.size());
        Assertions.assertSame(names.get(0), names.get(1).getDefinition());
        Assertions.assertSame(names.get(2), names.get(3).getDefinition());
        Assertions.assertNotSame(names.get(0), names.get(4).getDefinition());
        Assertions.assertNotSame(names.get(2), names.get(4).getDefinition());
    }

    @Test
    public void databaseCompositeTypeResolvesForLocalVariableAndCast() throws Exception {
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        BasicSQLDialect dialect = BasicSQLDialect.INSTANCE;
        DBSTypeDescriptor fieldType = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(fieldType.getKind()).thenReturn(DBSTypeDescriptor.Kind.SIMPLE);
        Mockito.when(fieldType.getTypeName()).thenReturn("integer");
        DBSTypeDescriptor compositeType = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(compositeType.getKind()).thenReturn(DBSTypeDescriptor.Kind.COMPOSITE);
        Mockito.when(compositeType.getTypeName()).thenReturn("app.custom_type");
        Mockito.when(compositeType.findCompositeMember(monitor, "field")).thenReturn(fieldType);
        Mockito.when(compositeType.findCompositeMember(monitor, "FIELD")).thenReturn(fieldType);
        DBSDataType dataType = Mockito.mock(DBSDataType.class, Mockito.withSettings().extraInterfaces(DBSTypedObjectEx2.class));
        Mockito.when(dataType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(((DBSTypedObjectEx2) dataType).getTypeDescriptor(monitor)).thenReturn(compositeType);

        DBPDataSource source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        DBPDataSourceContainer sourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(source.getSQLDialect()).thenReturn(dialect);
        Mockito.when(source.getContainer()).thenReturn(sourceContainer);
        Mockito.when(sourceContainer.isExtraMetadataReadEnabled()).thenReturn(true);
        Mockito.when(sourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());
        Mockito.when(((DBSObjectContainer) source).getDataSource()).thenReturn(source);
        DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
        DBSObjectContainer schema = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(catalog.getChild(monitor, "APP")).thenReturn(schema);
        Mockito.when(schema.getChild(monitor, "CUSTOM_TYPE")).thenReturn(dataType);
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(defaults.getDefaultCatalog()).thenReturn(catalog);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(executionContext.getDataSource()).thenReturn(source);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());

        SQLQueryRecognitionContext declarationContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        String declarationSql = "BEGIN DECLARE v app.custom_type; SELECT v.field; END";
        SQLQueryModel declarationModel = SQLQueryModelRecognizer.recognizeQuery(declarationContext, declarationSql);
        Assertions.assertNotNull(declarationModel);
        SQLQueryLocalVariableInfo local = declarationModel.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("v") && symbol.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .map(symbol -> (SQLQueryLocalVariableInfo) symbol.getDefinition())
            .findFirst().orElseThrow();
        SQLQueryVariableDeclarationStatement declaration = Assertions.assertInstanceOf(
            SQLQueryVariableDeclarationStatement.class, declarationModel.findNodeContaining(declarationSql.indexOf("DECLARE") + 1));
        Assertions.assertSame(local.type(), declaration.getDeclarations().getFirst().getType());
        Assertions.assertEquals(DBPDataKind.STRUCT, local.type().getDataKind());
        Assertions.assertEquals("integer", local.type().findNamedMemberType(monitor, "field").getDisplayName());
        Assertions.assertTrue(declarationContext.getProblems().isEmpty(), declarationContext.getProblems().toString());

        SQLQueryRecognitionContext castContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel castModel = SQLQueryModelRecognizer.recognizeQuery(castContext, "SELECT 1::app.custom_type AS result");
        Assertions.assertNotNull(castModel);
        SQLQueryRowsSourceModel result = Assertions.assertInstanceOf(SQLQueryRowsSourceModel.class, castModel.getQueryModel());
        SQLQueryExprType castType = result.getRowsDataContext().getColumnsList().getFirst().type;
        Assertions.assertEquals(DBPDataKind.STRUCT, castType.getDataKind());
        Assertions.assertEquals("integer", castType.findNamedMemberType(monitor, "field").getDisplayName());
        Assertions.assertTrue(castContext.getProblems().isEmpty(), castContext.getProblems().toString());

        SQLQueryRecognitionContext parameterContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel parameterModel = SQLQueryModelRecognizer.recognizeQuery(parameterContext,
            "CREATE FUNCTION f(x app.custom_type) RETURNS int AS $body$ BEGIN SELECT x.field; END; $body$ LANGUAGE sql");
        Assertions.assertNotNull(parameterModel);
        SQLQueryLocalVariableInfo parameter = parameterModel.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("x") && symbol.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .map(symbol -> (SQLQueryLocalVariableInfo) symbol.getDefinition())
            .findFirst().orElseThrow();
        Assertions.assertEquals(DBPDataKind.STRUCT, parameter.type().getDataKind());
        Assertions.assertTrue(parameterContext.getProblems().isEmpty(), parameterContext.getProblems().toString());

        SQLQueryRecognitionContext routineContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel routineModel = SQLQueryModelRecognizer.recognizeQuery(routineContext,
            "CREATE FUNCTION f(app.custom_type, x app.custom_type) RETURNS app.custom_type " +
                "AS BEGIN SELECT x.field; END");
        Assertions.assertNotNull(routineModel);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, routineModel.getQueryModel());
        Assertions.assertEquals(2, routine.getArguments().size());
        Assertions.assertNull(routine.getArguments().getFirst().declaration());
        Assertions.assertNotNull(routine.getArguments().getLast().declaration());
        Assertions.assertEquals(DBPDataKind.STRUCT, routine.getArguments().getFirst().resolvedType().getDataKind());
        Assertions.assertEquals(DBPDataKind.STRUCT, routine.getReturnTypes().getFirst().getDataKind());
        Assertions.assertEquals(DBPDataKind.STRUCT, routine.getArguments().getLast().resolvedType().getDataKind());
        assertTypeNamesBound(routineModel, dataType, 3);

        SQLQueryRecognitionContext tableReturnContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel tableReturnModel = SQLQueryModelRecognizer.recognizeQuery(tableReturnContext,
            "CREATE FUNCTION f() RETURNS TABLE (out_value app.custom_type, app.custom_type) " +
                "AS BEGIN SELECT 1; END");
        Assertions.assertNotNull(tableReturnModel);
        SQLQueryRoutineCreateModel tableReturn = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, tableReturnModel.getQueryModel());
        Assertions.assertEquals(2, tableReturn.getReturnTableTypes().size());
        Assertions.assertTrue(tableReturn.getReturnTableTypes().stream().allMatch(t -> t.getDataKind() == DBPDataKind.STRUCT));
        assertTypeNamesBound(tableReturnModel, dataType, 2);

        SQLQueryRecognitionContext tableContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel tableModel = SQLQueryModelRecognizer.recognizeQuery(tableContext,
            "CREATE TABLE typed_table (value app.custom_type, derived int DEFAULT value.field)");
        Assertions.assertNotNull(tableModel);
        SQLQueryTableCreateModel table = Assertions.assertInstanceOf(SQLQueryTableCreateModel.class, tableModel.getQueryModel());
        Assertions.assertEquals(DBPDataKind.STRUCT, table.getColumns().getFirst().getDeclaredColumnType().getDataKind());
        assertTypeNamesBound(tableModel, dataType, 1);
        Assertions.assertTrue(tableContext.getProblems().isEmpty(), tableContext.getProblems().toString());

        SQLQueryRecognitionContext localTableContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel localTableModel = SQLQueryModelRecognizer.recognizeQuery(localTableContext,
            "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (v app.custom_type); SELECT v.field FROM scratch; END");
        Assertions.assertNotNull(localTableModel);
        SQLQueryLocalVariableInfo scratch = localTableModel.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("scratch") && entry.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .map(entry -> (SQLQueryLocalVariableInfo) entry.getDefinition()).findFirst().orElseThrow();
        Assertions.assertEquals(DBPDataKind.STRUCT, scratch.rows().getRowsDataContext().getColumnsList().getFirst().type.getDataKind());
        assertTypeNamesBound(localTableModel, dataType, 1);

        SQLQueryRecognitionContext alterContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel alterModel = SQLQueryModelRecognizer.recognizeQuery(alterContext,
            "ALTER TABLE missing_table ADD COLUMN v app.custom_type");
        Assertions.assertNotNull(alterModel);
        SQLQueryTableAlterModel alter = Assertions.assertInstanceOf(SQLQueryTableAlterModel.class, alterModel.getQueryModel());
        Assertions.assertEquals(DBPDataKind.STRUCT,
            alter.getAlterActions().getFirst().getColumnSpec().getDeclaredColumnType().getDataKind());
        assertTypeNamesBound(alterModel, dataType, 1);

        SQLQueryRecognitionContext fallbackContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel fallbackModel = SQLQueryModelRecognizer.recognizeQuery(fallbackContext, "SELECT 1::missing_type AS result");
        Assertions.assertNotNull(fallbackModel);
        SQLQueryRowsSourceModel fallbackResult = Assertions.assertInstanceOf(SQLQueryRowsSourceModel.class, fallbackModel.getQueryModel());
        SQLQueryExprType fallback = fallbackResult.getRowsDataContext().getColumnsList().getFirst().type;
        Assertions.assertEquals(DBPDataKind.UNKNOWN, fallback.getDataKind());
        Assertions.assertEquals("missing_type", fallback.getDisplayName());
    }

    private static void assertTypeNamesBound(SQLQueryModel model, DBSDataType type, int expectedCount) {
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("custom_type")).toList();
        Assertions.assertEquals(expectedCount, names.size());
        for (SQLQuerySymbolEntry name : names) {
            Assertions.assertEquals(SQLQuerySymbolClass.TYPE, name.getSymbolClass());
            SQLQuerySymbolByDbObjectDefinition definition = Assertions.assertInstanceOf(
                SQLQuerySymbolByDbObjectDefinition.class, name.getDefinition());
            Assertions.assertSame(type, definition.getDbObject());
        }
    }

    @Test
    public void regularAndTemporaryTableCreationHaveDifferentStatementModels() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TABLE permanent (id int); " +
            "CREATE TEMP TABLE scratch (id int); END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        SQLQueryRoutineStatementModel routine = Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        SQLQueryStatementBlock block = Assertions.assertInstanceOf(SQLQueryStatementBlock.class, body.getStatements().getFirst());
        SQLQueryStatementSequence statements = Assertions.assertInstanceOf(
            SQLQueryStatementSequence.class, block.getSections().getFirst()
        );
        SQLQueryEmbeddedStatement create = Assertions.assertInstanceOf(
            SQLQueryEmbeddedStatement.class, statements.getStatements().getFirst()
        );
        Assertions.assertInstanceOf(SQLQueryTableCreateModel.class, create.getSql());
        Assertions.assertInstanceOf(SQLQueryRelationDeclarationStatement.class, statements.getStatements().get(1));
    }

    @Test
    public void visitorDistinguishesProgrammaticalAndBodyStatementTypes() {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);

        @SuppressWarnings("unchecked")
        SQLQueryNodeModelVisitor<String, String> visitor = (SQLQueryNodeModelVisitor<String, String>) Proxy.newProxyInstance(
            SQLQueryNodeModelVisitor.class.getClassLoader(), new Class<?>[] { SQLQueryNodeModelVisitor.class },
            (proxy, method, args) -> method.getName()
        );
        Assertions.assertEquals("visitRoutineCreate", model.getQueryModel().apply(visitor, ""));
        Assertions.assertEquals("visitAssignmentStatement", model.findNodeContaining(sql.indexOf("SET x") + 1).apply(visitor, ""));
    }

    private static void assertCancellationAtEveryCheckpoint(@NotNull String sql) {
        CountingProgressMonitor completedMonitor = new CountingProgressMonitor(Integer.MAX_VALUE);
        SQLQueryModel completedModel = SQLQueryModelRecognizer.recognizeQuery(createContext(completedMonitor), sql);
        Assertions.assertTrue(completedModel != null && completedModel.getQueryModel() != null, sql);

        int cancellationPoint = 0;
        while (cancellationPoint < completedMonitor.getCheckCount()) {
            Assertions.assertNull(
                SQLQueryModelRecognizer.recognizeQuery(createContext(new CountingProgressMonitor(cancellationPoint)), sql),
                sql + " returned a model when cancelled at checkpoint " + cancellationPoint + " but should have been cancelled"
            );
            cancellationPoint++;
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new CountingProgressMonitor(cancellationPoint)),
            sql
        );
        Assertions.assertTrue(
            model != null && model.getQueryModel() != null,
            sql + " didn't return a model when cancelled at checkpoint " + cancellationPoint + " but should have"
        );
    }

    private static void assertUnsupportedStatementCancellationAtEveryCheckpoint(@NotNull String sql) {
        CountingProgressMonitor completedMonitor = new CountingProgressMonitor(Integer.MAX_VALUE);
        SQLQueryModel completedModel = SQLQueryModelRecognizer.recognizeQuery(createContext(completedMonitor), sql);
        Assertions.assertTrue(completedModel != null && completedModel.getQueryModel() == null, sql);

        int cancellationPoint = 0;
        while (cancellationPoint < completedMonitor.getCheckCount()) {
            Assertions.assertNull(
                SQLQueryModelRecognizer.recognizeQuery(createContext(new CountingProgressMonitor(cancellationPoint)), sql),
                sql + " returned a model when cancelled at checkpoint " + cancellationPoint + " but should have been cancelled"
            );
            cancellationPoint++;
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new CountingProgressMonitor(cancellationPoint)),
            sql
        );
        Assertions.assertTrue(
            model != null && model.getQueryModel() == null,
            sql + " didn't return a fallback model at checkpoint " + cancellationPoint + " but should have"
        );
    }

    @NotNull
    private static SQLQueryVariableStatementModel recognizeVariableScope(@NotNull String sql) {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        return Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());
    }

    @NotNull
    private static SQLQueryRecognitionContext createContext(@NotNull VoidProgressMonitor monitor) {
        SQLDialect dialect = BasicSQLDialect.INSTANCE;
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        return new SQLQueryRecognitionContext(monitor, null, false, false, syntaxManager, dialect);
    }

    private static class CountingProgressMonitor extends VoidProgressMonitor {
        private final int cancellationPoint;
        private int checkCount;

        private CountingProgressMonitor(int cancellationPoint) {
            this.cancellationPoint = cancellationPoint;
        }

        @Override
        public boolean isCanceled() {
            return this.checkCount++ >= this.cancellationPoint;
        }

        private int getCheckCount() {
            return this.checkCount;
        }
    }
}
