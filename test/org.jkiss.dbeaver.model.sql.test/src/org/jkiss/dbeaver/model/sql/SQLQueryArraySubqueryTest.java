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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueFunctionExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueSubqueryExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsProjectionModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableProcModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQuerySelectionResultColumnSpec;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class SQLQueryArraySubqueryTest extends DBeaverUnitTest {
    @Test
    void arraySubqueryWithInnerCteRetainsTheCteAndItsResultType() {
        String sql = "SELECT ARRAY(WITH selected AS (SELECT 1 AS id) SELECT id FROM selected) AS ids";
        SQLQueryModel model = recognize(dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL), sql);
        var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("ARRAY(") + 1));
        Assertions.assertTrue(array.isArraySubqueryConstructor());
        Assertions.assertEquals(DBPDataKind.ARRAY, array.getValueType().getDataKind());
        var query = Assertions.assertInstanceOf(SQLQueryValueSubqueryExpression.class, array.getOperands().getFirst());
        var cte = Assertions.assertInstanceOf(SQLQueryRowsCteModel.class, query.getSource());
        Assertions.assertEquals(2, cte.getAllQueries().size());
        Assertions.assertEquals(DBPDataKind.NUMERIC, cte.getRowsDataContext().getColumnsList().getFirst().type.getDataKind());
        Assertions.assertTrue(model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("selected")).count() >= 2);
    }

    @Test
    void scalarSubqueryWithInnerCteRetainsTheCteAndItsResultType() {
        String sql = "SELECT (WITH selected AS (SELECT 1 AS id) SELECT id FROM selected) AS id";
        SQLQueryModel model = recognize(BasicSQLDialect.INSTANCE, sql);
        var projection = Assertions.assertInstanceOf(SQLQueryRowsProjectionModel.class, model.getQueryModel());
        var column = Assertions.assertInstanceOf(SQLQuerySelectionResultColumnSpec.class,
            projection.getResult().getSublists().getFirst());
        var query = Assertions.assertInstanceOf(SQLQueryValueSubqueryExpression.class, column.getValueExpression());
        var cte = Assertions.assertInstanceOf(SQLQueryRowsCteModel.class, query.getSource());
        Assertions.assertEquals(2, cte.getAllQueries().size());
        Assertions.assertEquals(DBPDataKind.NUMERIC, projection.getRowsDataContext().getColumnsList().getFirst().type.getDataKind());
    }

    @Test
    void postgresArraySubqueryHasAnIndexableValueType() throws Exception {
        SQLDialect dialect = dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL);
        for (String sql : List.of(
            "SELECT ARRAY(SELECT 1) AS numbers",
            "WITH values_cte AS (SELECT 1 AS n) SELECT ARRAY(SELECT n FROM values_cte) numbers"
        )) {
            SQLQueryModel model = recognize(dialect, sql);
            var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
                model.findNodeContaining(sql.indexOf("ARRAY(") + 1), sql);
            Assertions.assertTrue(array.isArraySubqueryConstructor(), sql);
            Assertions.assertEquals(1, array.getOperands().size(), sql);
            Assertions.assertEquals(DBPDataKind.ARRAY, array.getValueType().getDataKind(), sql);
            Assertions.assertNotNull(array.getValueType().findIndexedItemType(new VoidProgressMonitor(), 1, null), sql);
            Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("numbers")), sql);
        }
    }

    @Test
    void arraySubqueryModesHandleNestedArraysDifferently() throws Exception {
        for (SQLDialectFeatures.ArraySubquerySemantics mode : List.of(
            SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL,
            SQLDialectFeatures.ArraySubquerySemantics.NO_NESTED_ARRAY,
            SQLDialectFeatures.ArraySubquerySemantics.SQL_STANDARD_SINGLE_COLUMN
        )) {
            SQLDialect dialect = dialectWithMode(mode);
            for (String sql : List.of(
                "SELECT ARRAY(SELECT 1) AS elements",
                "SELECT ARRAY((SELECT 1)) AS parenthesized",
                "SELECT ARRAY(SELECT ARRAY(SELECT 1)) AS nested"
            )) {
                SQLQueryModel model = recognize(dialect, sql);
                var outer = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
                    model.findNodeContaining(sql.indexOf("ARRAY(") + 1), mode + ": " + sql);
                Assertions.assertTrue(outer.isArraySubqueryConstructor());
                if (!sql.contains("nested")) {
                    Assertions.assertEquals(DBPDataKind.ARRAY, outer.getValueType().getDataKind(), sql);
                }
                if (sql.contains("nested")) {
                    var inner = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
                        model.findNodeContaining(sql.lastIndexOf("ARRAY(") + 1), mode + ": " + sql);
                    Assertions.assertTrue(inner.isArraySubqueryConstructor());
                    if (mode == SQLDialectFeatures.ArraySubquerySemantics.NO_NESTED_ARRAY) {
                        Assertions.assertEquals(DBPDataKind.UNKNOWN, outer.getValueType().getDataKind());
                    } else {
                        Assertions.assertEquals(DBPDataKind.ARRAY, outer.getValueType().getDataKind());
                        var element = outer.getValueType().findIndexedItemType(new VoidProgressMonitor(), 1, null);
                        Assertions.assertNotNull(element);
                        Assertions.assertEquals(mode == SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL
                            ? DBPDataKind.NUMERIC : DBPDataKind.ARRAY, element.getDataKind());
                    }
                }
            }
        }
    }

    @Test
    void arraySubqueryInsideScalarFunctionRetainsBothOperands() {
        String sql = "SELECT wrap_array(ARRAY(SELECT 1), ARRAY(SELECT 2)) AS result";
        SQLDialect dialect = dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.NO_NESTED_ARRAY);
        SQLQueryModel model = recognize(dialect, sql);
        var function = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("wrap_array") + 1));
        Assertions.assertEquals(2, function.getOperands().size());
        for (var operand : function.getOperands()) {
            var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class, operand);
            Assertions.assertTrue(array.isArraySubqueryConstructor());
            Assertions.assertEquals(DBPDataKind.ARRAY, operand.getValueType().getDataKind());
        }
    }

    @Test
    void defaultDialectKeepsOrdinaryArrayFunctionFallback() {
        for (String sql : List.of(
            "SELECT ARRAY(1) AS elements", "SELECT ARRAY(SELECT 1) AS elements",
            "SELECT ARRAY((SELECT 1)) AS elements"
        )) {
            SQLQueryModel model = recognize(BasicSQLDialect.INSTANCE, sql);
            Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
                model.findNodeContaining(sql.indexOf("ARRAY(") + 1), sql);
            Assertions.assertFalse(((SQLQueryValueFunctionExpression) model.findNodeContaining(sql.indexOf("ARRAY(") + 1))
                .isArraySubqueryConstructor());
        }
    }

    @Test
    void arraySubqueryDoesNotDiscardExtraQueryColumnsOrOrdinaryFunctionOperands() {
        SQLDialect dialect = dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL);
        String sql = "SELECT ARRAY(SELECT 1, 2) AS invalid";
        SQLQueryModel model = recognize(dialect, sql);
        var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("ARRAY(") + 1));
        Assertions.assertTrue(array.isArraySubqueryConstructor());
        Assertions.assertEquals(1, array.getOperands().size());
        Assertions.assertEquals(DBPDataKind.UNKNOWN, array.getValueType().getDataKind());

        sql = "SELECT ARRAY(1, 2) AS values_list";
        model = recognize(dialect, sql);
        array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("ARRAY(") + 1));
        Assertions.assertFalse(array.isArraySubqueryConstructor());
        Assertions.assertEquals(2, array.getOperands().size());

        sql = "SELECT wrap_array((SELECT 1)) AS scalar_argument";
        model = recognize(dialect, sql);
        var function = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("wrap_array") + 1));
        Assertions.assertFalse(function.isArraySubqueryConstructor());
        Assertions.assertEquals(1, function.getOperands().size());
    }

    @Test
    void fromArrayRetainsTheInnerQueryWithoutTreatingItsElementsAsTableRows() {
        SQLDialect dialect = dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL);
        for (String sql : List.of(
            "SELECT * FROM ARRAY(SELECT i.id FROM items i) a",
            "SELECT * FROM ARRAY(SELECT i. FROM items i) a"
        )) {
            SQLQueryModel model = recognize(dialect, sql);
            var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
                model.findNodeContaining(sql.indexOf("ARRAY(") + 1), sql);
            Assertions.assertTrue(array.isArraySubqueryConstructor(), sql);
            var projection = Assertions.assertInstanceOf(SQLQueryRowsProjectionModel.class, model.getQueryModel());
            var from = Assertions.assertInstanceOf(SQLQueryRowsCorrelatedSourceModel.class, projection.getFromSource());
            var tableProc = Assertions.assertInstanceOf(SQLQueryRowsTableProcModel.class, from.getSource());
            Assertions.assertSame(array, tableProc.getFunctionExpression());
            Assertions.assertTrue(tableProc.getRowsDataContext().getColumnsList().isEmpty());
            Assertions.assertInstanceOf(SQLQueryRowsTableDataModel.class,
                model.findNodeContaining(sql.indexOf("items") + 1), sql);
            Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("i")), sql);
        }
    }

    @Test
    void fromFunctionRetainsNestedArraySubquery() {
        String sql = "SELECT * FROM unnest(ARRAY(SELECT i.id FROM items i)) AS item_id";
        SQLDialect dialect = dialectWithMode(SQLDialectFeatures.ArraySubquerySemantics.MULTIDIMENSIONAL);
        SQLQueryModel model = recognize(dialect, sql);
        Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("unnest") + 1));
        var array = Assertions.assertInstanceOf(SQLQueryValueFunctionExpression.class,
            model.findNodeContaining(sql.indexOf("ARRAY(") + 1));
        Assertions.assertTrue(array.isArraySubqueryConstructor());
        Assertions.assertInstanceOf(SQLQueryRowsTableDataModel.class,
            model.findNodeContaining(sql.indexOf("items") + 1));
    }

    @NotNull
    private static SQLDialect dialectWithMode(@NotNull SQLDialectFeatures.ArraySubquerySemantics mode) {
        SQLDialectFeatures features = Mockito.mock(SQLDialectFeatures.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(mode).when(features).getArraySubquerySemantics();
        return new BasicSQLDialect() {
            @NotNull
            @Override
            public SQLDialectFeatures getDialectFeatures() {
                return features;
            }
        };
    }

    @NotNull
    private static SQLQueryModel recognize(@NotNull SQLDialect dialect, @NotNull String sql) {
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            new SQLQueryRecognitionContext(new VoidProgressMonitor(), null, false, false, syntax, dialect), sql);
        Assertions.assertNotNull(model, sql);
        return model;
    }
}
