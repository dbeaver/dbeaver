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
package org.jkiss.dbeaver.model.lsm.test;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SQLStandardGrammarAcceptanceTest extends DBeaverUnitTest {
    private static final List<GrammarCase> CASES = List.of(
        new GrammarCase("dbeaver#37939 GENERATED identity with UNIQUE", """
            CREATE TABLE IF NOT EXISTS requirements (
              id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              name varchar(255) NOT NULL,
              unique (id, name)
            );
            """),
        new GrammarCase("dbeaver#36357 INNER JOIN LATERAL", """
            select *
            from
              pg_class
              inner join lateral (
                select * from pg_attribute where attrelid = pg_class.oid
              ) t on true
            """),
        new GrammarCase("dbeaver#36541 LEFT JOIN LATERAL", """
            SELECT * FROM prt1 t1 LEFT JOIN LATERAL
              (SELECT t2.a AS t2a, t3.a AS t3a, least(t1.a,t2.a,t3.b) FROM prt1 t2 JOIN prt2 t3 ON (t2.a = t3.b)) ss
              ON t1.a = ss.t2a WHERE t1.b = 0 ORDER BY t1.a;
            """),
        new GrammarCase("dbeaver#37431 STRAIGHT_JOIN", """
            SELECT
                mps.id AS mpSupplyID,
                mpsp.id AS mpSupplyProductID,
                mpsp.mpProductCardID,
                mps.mpWarehouseID,
                mpst.id AS mpStockID,
                mpstd.id AS mpStockDateID,
                mpstd.mpQuantitySupply,
                GROUP_CONCAT(mpsp.id) AS mpSupplyProductIDs,
                SUM(mpsp.productNumApi) AS productNumApi
                FROM
                sMarketplace_supply AS mps
                STRAIGHT_JOIN sPlace AS pl ON (pl.mpSupplyID=mps.id)
                STRAIGHT_JOIN sSerial AS s ON (s.placeID=pl.placeID)
                STRAIGHT_JOIN sMarketplace_supply_product AS mpsp ON (mpsp.mpSupplyID=mps.id)
                STRAIGHT_JOIN sMarketplace_stock AS mpst ON (mpst.mpWarehouseID=mps.mpWarehouseID AND mpst.mpProductCardID=mpsp.mpProductCardID)
                STRAIGHT_JOIN sMarketplace_stock_date AS mpstd ON (mpstd.mpStockID=mpst.id AND mpstd.mpStockDate=mps.mpSupplyDateFact)
                WHERE 1=1
                #AND mpsp.mpProductCardID=85

                GROUP BY mpstd.id
                HAVING mpQuantitySupply!=productNumApi
            """),
        new GrammarCase("dbeaver#35593 nested Array datatype", """
            CREATE TABLE test_arrays
            (
                id UInt32,
                int_array Array(Int32),
                string_array Array(String),
                float_array Array(Float64),
                nested_int_array Array(Array(Int32)),
                nested_string_array Array(Array(String)),
                boolean_array Array(Bool)
            ) ENGINE = MergeTree()
            ORDER BY id;
            """),
        new GrammarCase("dbeaver#35125 / dbeaver#37894 recursive CTE with VALUES", """
            with recursive all_data as (
                select *
                from (values(1, 1, 10),(1, 2, 3),(1, 3, 7),(1, 4, 1),(1, 5, 2),(2, 1, 6),(2, 2, 4),(2, 3, 5),(2, 4, 8),(2, 5, 1)) as s(gr, id, quantity)
            ),
            ct AS (
                select *, c.quantity AS sm
                from all_data AS c
                WHERE id = 1
                UNION ALL
                select c2.*, c.sm + c2.quantity AS sm
                from ct AS c
                JOIN all_data AS c2 ON c2.id = c.id + 1 AND c.gr = c2.gr
            )
            SELECT *
            FROM ct
            ORDER BY gr, id
            ;
            """),
        new GrammarCase("dbeaver#34924 UPDATE FROM aliases", """
            update tab1 t
            set name1 = t2.name1
            from tab2 t2
            where t.id = t2.id
            """),
        new GrammarCase("dbeaver#22336 GROUP_CONCAT ORDER BY SEPARATOR", """
            select
            u.username
            ,u.lov_user_status
            ,GROUP_CONCAT(m.name ORDER BY m.name SEPARATOR ', ') modules
            ,u.created
            ,u.updated
            ,u.lov_user_type
            ,u.last_login_date
            ,u.lov_user_status
            ,u.blocked
            from users u
            left join users_modules um on um.user_id = u.id
            left join modules m on um.module_id = m.id
            where
            u.lov_user_status = 'Active'
            Group by u.id
            ;
            """),
        new GrammarCase("dbeaver#22168 ROW_NUMBER OVER", """
            SELECT *, ROW_NUMBER()
            OVER (ORDER BY price) AS row_num
            FROM Persons;
            """),
        new GrammarCase("dbeaver#22773 ORDER BY i.*", "select * from account_invoice i order by i.*;"),
        new GrammarCase("dbeaver#20996 NOTNULL", "select * from mytable where 1 notnull;"),
        new GrammarCase("dbeaver#29316 DROP TABLE cache", "DROP TABLE cache;")
    );

    @TestFactory
    @NotNull
    Stream<DynamicTest> acceptsClosedIssueStatements() {
        return CASES.stream().map(testCase -> DynamicTest.dynamicTest(
            testCase.name(),
            () -> assertAccepted(testCase.sql())
        ));
    }

    private static void assertAccepted(@NotNull String sql) {
        var lexerErrors = new SyntaxErrorCollector();
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrors);

        var tokens = new CommonTokenStream(lexer);
        tokens.fill();

        var parserErrors = new SyntaxErrorCollector();
        var parser = new SQLStandardParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrors);
        parser.sqlQueries();

        Assertions.assertAll(
            () -> Assertions.assertTrue(lexerErrors.isEmpty(), () -> "Lexer errors:\n" + lexerErrors.describe()),
            () -> Assertions.assertTrue(parserErrors.isEmpty(), () -> "Parser errors:\n" + parserErrors.describe())
        );
    }

    private record GrammarCase(@NotNull String name, @NotNull String sql) {
    }

    private static final class SyntaxErrorCollector extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String message,
            @Nullable RecognitionException exception
        ) {
            errors.add("line " + line + ":" + charPositionInLine + " " + message);
        }

        boolean isEmpty() {
            return errors.isEmpty();
        }

        @NotNull
        String describe() {
            return String.join("\n", errors);
        }
    }
}
