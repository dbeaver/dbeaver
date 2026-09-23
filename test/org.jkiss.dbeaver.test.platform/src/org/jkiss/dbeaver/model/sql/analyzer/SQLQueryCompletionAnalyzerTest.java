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
package org.jkiss.dbeaver.model.sql.analyzer;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestResult;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.jkiss.dbeaver.model.sql.analyzer.builder.Builder.Consumer.empty;

public class SQLQueryCompletionAnalyzerTest extends DBeaverUnitTest {
    private static RequestResult modelDataRequest;

    @BeforeEach
    public void init() throws DBException {
        if (Platform.isRunning()) {
            modelDataRequest = RequestBuilder
                .tables(t -> {

                    t.table("table1", f -> {
                        f.attribute("attribute1");
                        f.attribute("attribute2");
                        f.attribute("attribute3");
                    });
                    t.table("table2", f -> {
                        f.attribute("attribute1");
                        f.attribute("attribute2");
                        f.attribute("attribute3");
                    });
                    t.table("table3", f -> {
                        f.attribute("attribute1");
                        f.attribute("attribute2");
                        f.attribute("attribute3");
                    });
                    t.table("tableNaMeA", f -> {
                        f.attribute("attribute-a");
                        f.attribute("attribute-A");
                        f.attribute("attribute-Aa");
                    });
                    t.table("tableNaMeB", f -> {
                        f.attribute("attribute-a");
                        f.attribute("attribute-A");
                        f.attribute("attribute-Aa");
                    });
                })
                .prepare();
        }
    }
    
    @Test
    public void testKeywordCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .empty()
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("SEL|");
            
            Assertions.assertTrue(proposals.contains("SELECT"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * |");
            
            Assertions.assertTrue(proposals.contains("FROM"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM T |");
            
            Assertions.assertTrue(proposals.contains("WHERE"));
        }
    }

    @Test
    public void testColumnNamesCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .tables(s -> {
                s.table("Table1", t -> {
                    t.attribute("Col1");
                    t.attribute("Col2");
                    t.attribute("Col3");
                });
                s.table("Table2", t -> {
                    t.attribute("Col4");
                    t.attribute("Col5");
                    t.attribute("Col6");
                });
                s.table("Table 3", t -> {
                    t.attribute("Col7");
                    t.attribute("Col8");
                    t.attribute("Col9");
                });
            })
            .prepare();

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT | FROM Table1");


            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 WHERE |");

            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 WHERE Table1.|");


            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 t WHERE t.|");

            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM \"Table 3\" t WHERE t.|");

            
            Assertions.assertTrue(proposals.contains("Col7"));
            Assertions.assertTrue(proposals.contains("Col8"));
            Assertions.assertTrue(proposals.contains("Col9"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t.| FROM Table1 t");

            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t2.| FROM Table1 t, Table2 t2");

            
            Assertions.assertTrue(proposals.contains("Col4"));
            Assertions.assertTrue(proposals.contains("Col5"));
            Assertions.assertTrue(proposals.contains("Col6"));
        }
    }

    @Test
    public void testColumnWithNonExistingAliases() throws DBException {
        final RequestResult request = RequestBuilder.tables(s -> {
            s.table("Table1", t -> {
                t.attribute("Col1");
                t.attribute("Col2");
            });
            s.table("Table2", t -> {
                t.attribute("Col4");
                t.attribute("Col5");
            });
        }).prepare();
        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Table1 join Table2 t on t.|", false);
            Assertions.assertTrue(proposals.contains("Col4"));
            Assertions.assertTrue(proposals.contains("Col5"));
        }
        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Table1 b join Table2 on b.|", false);
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
        }
    }

    @Test
    public void testColumnNamesExpandCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .tables(s -> {
                s.table("Table1", t -> {
                    t.attribute("Col1");
                    t.attribute("Col2");
                    t.attribute("Col3");
                });
            })
            .prepare();

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT *| FROM Table1", false);

            
            Assertions.assertTrue(proposals.contains("Col1, Col2, Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t.*| FROM Table1 t", false);

            
            Assertions.assertTrue(proposals.contains("t.Col1, t.Col2, t.Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT Table1.*| FROM Table1", false);

            
            Assertions.assertTrue(proposals.contains("Col1, Col2, Col3"));
        }
    }

    @Test
    public void testTableNamesCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .tables(s -> {
                s.table("Table1", empty());
                s.table("Table2", empty());
                s.table("Table3", empty());
                s.table("Tbl4", empty());
                s.table("Tbl5", empty());
                s.table("Tbl6", empty());
            })
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM |");
            Assertions.assertTrue(proposals.size() >= 3);
            Assertions.assertTrue(proposals.contains("Table1 t"));
            Assertions.assertTrue(proposals.contains("Table2 t"));
            Assertions.assertTrue(proposals.contains("Table3 t"));
            Assertions.assertTrue(proposals.contains("Tbl4 t"));
            Assertions.assertTrue(proposals.contains("Tbl5 t"));
            Assertions.assertTrue(proposals.contains("Tbl6 t"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Tb|");
            Assertions.assertFalse(proposals.contains("Table1 t"));
            Assertions.assertFalse(proposals.contains("Table2 t"));
            Assertions.assertFalse(proposals.contains("Table3 t"));
            Assertions.assertTrue(proposals.contains("Tbl4 t"));
            Assertions.assertTrue(proposals.contains("Tbl5 t"));
            Assertions.assertTrue(proposals.contains("Tbl6 t"));
        }
    }

    @Test
    public void testSchemaTableNamesCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .schemas(d -> {
                d.schema("Schema1", s -> {
                    s.table("Table1", empty());
                    s.table("Table2", empty());
                    s.table("Table3", empty());
                });
                d.schema("Schema2", s -> {
                    s.table("Table4", empty());
                    s.table("Table5", empty());
                    s.table("Table6", empty());
                });
            })
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Sch|");
            
            Assertions.assertTrue(proposals.contains("Schema1"));
            Assertions.assertTrue(proposals.contains("Schema2"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Schema1.|");
            
            Assertions.assertTrue(proposals.contains("Table1 t"));
            Assertions.assertTrue(proposals.contains("Table2 t"));
            Assertions.assertTrue(proposals.contains("Table3 t"));
        }
    }

    @Test
    public void testDatabaseSchemaTableNamesCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .databases(x -> {
                x.database("Database1", d -> {
                    d.schema("Schema1", s -> {
                        s.table("Table1", empty());
                        s.table("Table2", empty());
                        s.table("Table3", empty());
                    });
                });
                x.database("Database2", d -> {
                    d.schema("Schema2", s -> {
                        s.table("Table4", empty());
                        s.table("Table5", empty());
                        s.table("Table6", empty());
                    });
                });
                x.database("Database3", d -> {
                    d.schema("a.schema", s -> {
                        s.table("a.table", empty());
                    });
                });
            })
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Da|");
            
            Assertions.assertTrue(proposals.contains("Database1"));
            Assertions.assertTrue(proposals.contains("Database2"));
            Assertions.assertTrue(proposals.contains("Database3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database1.|");
            
            Assertions.assertTrue(proposals.contains("Schema1"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database1.Schema1.|");
            
            Assertions.assertTrue(proposals.contains("Table1 t"));
            Assertions.assertTrue(proposals.contains("Table2 t"));
            Assertions.assertTrue(proposals.contains("Table3 t"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database3.|");
            
            Assertions.assertTrue(proposals.contains("\"a.schema\""));
            
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database3.\"a.schema\".|");
            
            Assertions.assertTrue(proposals.contains("\"a.table\" t"));
            
        }
    }

    @Test
    public void testColumnsQuotedNamesCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .databases(x -> {
                x.database("Database1", d -> {
                    d.schema("Schema1", s -> {
                        s.table("Table1", t -> {
                            t.attribute("Col1");
                            t.attribute("Col2");
                            t.attribute("Col3");
                        });
                    });
                });
            })
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("SELECT | FROM Database1.Schema1.Table1");
            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT | FROM \"Database1\".Schema1.\"Table1\"");
            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT | FROM \"Database1\".\"Schema1\".\"Table1\"");
            
            Assertions.assertTrue(proposals.contains("Col1"));
            Assertions.assertTrue(proposals.contains("Col2"));
            Assertions.assertTrue(proposals.contains("Col3"));
        }
    }

    @Test
    public void testColumnsCompletionInUpdate() throws DBException {
        final RequestResult request = RequestBuilder
            .databases(x -> {
                x.database(
                    "db", d -> {
                        d.schema(
                            "sch", s -> {
                                s.table(
                                    "tbl", t -> {
                                        t.attribute("col1");
                                        t.attribute("col2");
                                        t.attribute("col3");
                                    }
                                );
                            }
                        );
                    }
                );
            })
            .prepare();

        {
            final Set<String> proposals = request.requestNewStrings("UPDATE db.sch.tbl t SET |");
            Assertions.assertTrue(proposals.contains("col1"));
            Assertions.assertTrue(proposals.contains("col2"));
            Assertions.assertTrue(proposals.contains("col3"));
        }
    }
    
    @Test
    public void testCompleteTablesWithAliasesPositive() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE |");
        
        Assertions.assertTrue(proposals.contains("a.attribute1"));
        Assertions.assertTrue(proposals.contains("a.attribute2"));
        Assertions.assertTrue(proposals.contains("a.attribute3"));
        Assertions.assertTrue(proposals.contains("b.attribute1"));
        Assertions.assertTrue(proposals.contains("b.attribute2"));
        Assertions.assertTrue(proposals.contains("b.attribute3"));

        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.|");
        Assertions.assertTrue(proposals.contains("attribute1"));
        Assertions.assertTrue(proposals.contains("attribute2"));
        Assertions.assertTrue(proposals.contains("attribute3"));

        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE b.|");
        Assertions.assertTrue(proposals.contains("attribute1"));
        Assertions.assertTrue(proposals.contains("attribute2"));
        Assertions.assertTrue(proposals.contains("attribute3"));
        
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND |");
        Assertions.assertTrue(proposals.contains("a.attribute1"));
        Assertions.assertTrue(proposals.contains("a.attribute2"));
        Assertions.assertTrue(proposals.contains("a.attribute3"));
        Assertions.assertTrue(proposals.contains("b.attribute1"));
        Assertions.assertTrue(proposals.contains("b.attribute2"));
        Assertions.assertTrue(proposals.contains("b.attribute3"));
        
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND b.|");
        Assertions.assertTrue(proposals.contains("attribute1"));
        Assertions.assertTrue(proposals.contains("attribute2"));
        Assertions.assertTrue(proposals.contains("attribute3"));

        // all
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        Assertions.assertTrue(proposals.contains("a.\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("a.\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("a.\"attribute-Aa\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-Aa\""));

        // a
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assertions.assertTrue(proposals.contains("\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("\"attribute-Aa\""));

        // b
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assertions.assertTrue(proposals.contains("\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("\"attribute-Aa\""));
    }
    
    @Test
    public void testCompleteTablesWithAliasesQuotedPositive() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        // alias from a and b
        Assertions.assertTrue(proposals.contains("a.\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("a.\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("a.\"attribute-Aa\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("b.\"attribute-Aa\""));
        // alias from a
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assertions.assertTrue(proposals.contains("\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("\"attribute-Aa\""));
        // alias from b
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assertions.assertTrue(proposals.contains("\"attribute-a\""));
        Assertions.assertTrue(proposals.contains("\"attribute-A\""));
        Assertions.assertTrue(proposals.contains("\"attribute-Aa\""));
    }

    @Test
    public void testCompleteTablesByAliaseNegative() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE c.|");
        Assertions.assertTrue(proposals.isEmpty());
    }

    @Test
    public void testPro3816ColumnsBetweenCommasHaveNoGeneratedAliases() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("J1_TBL", t -> {
                t.attribute("i");
                t.attribute("j");
            });
            s.table("J2_TBL", t -> {
                t.attribute("i");
                t.attribute("k");
            });
        }).prepare();

        Set<String> proposals = request.requestNewStrings(
            "SELECT '' AS \"xxx\", | FROM J1_TBL t1 CROSS JOIN J2_TBL t2"
        );
        assertContains(proposals, "t1.i", "t1.j", "t2.i", "t2.k");
        Assertions.assertTrue(proposals.stream().noneMatch(p -> p.contains(" AS ") || p.contains("t1.t1.") ||
            p.contains("t2.t2.")), proposals::toString);
    }

    @Test
    public void testPro3829ParenthesizedJoinScopes() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("x", t -> {
                t.attribute("x1");
                t.attribute("x2");
            });
            s.table("y", t -> {
                t.attribute("y1");
                t.attribute("y2");
            });
        }).prepare();

        Set<String> innerJoin = request.requestNewStrings(
            "select * from (x left join y on (|x1 = y1)) left join x xx(xx1, xx2) " +
                "on (x1 = xx1 and y2 is not null)"
        );
        assertContains(innerJoin, "x1", "x2", "y1", "y2");
        assertNotContains(innerJoin, "xx.xx1", "xx.xx2");

        Set<String> outerJoin = request.requestNewStrings(
            "select * from (x left join y on (x1 = y1)) left join x xx(xx1, xx2) " +
                "on (|x1 = xx1 and xx2 is not null)"
        );
        assertContains(outerJoin, "x1", "x2", "y1", "y2", "xx.xx1", "xx.xx2");

        // following scenarios cover presently not-properly-supported forms of value-expressions their support requires
        // TODO not just slight grammar adjustments, but certain grammar refactoring and semantic resolution improvement
        //      (valueExpression vs rowValueConstructor ambiguity)

//        Set<String> simpleCorrelatedColumns = request.requestNewStrings(
//            "select * from x left join x xx(xx1, xx2) on (x1 = xx1 and xx.|)"
//        );
//        assertContains(simpleCorrelatedColumns, "xx1", "xx2");

//        Set<String> correlatedColumns = request.requestNewStrings(
//            "select * from (x left join y on (x1 = y1)) left join x xx(xx1, xx2) " +
//                "on (x1 = xx1 and xx.|)"
//        );
//        assertContains(correlatedColumns, "xx1", "xx2");
//        assertNotContains(correlatedColumns, "x1", "x2");
    }

    @Test
    public void testPro3830PartialTableName() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("y", empty());
            s.table("yy", empty());
            s.table("xx", empty());
        }).prepare();

        Set<String> proposals = request.requestNewStrings("select * from y|");
        assertContains(proposals, "y y2", "yy y2");
        assertNotContains(proposals, "xx x");
    }

    @Test
    public void testPro3831UsingColumns() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("J1_TBL", t -> {
                t.attribute("i");
                t.attribute("j");
            });
            s.table("J2_TBL", t -> {
                t.attribute("i");
                t.attribute("k");
            });
        }).prepare();

        Set<String> proposals = request.requestNewStrings(
            "SELECT * FROM J1_TBL JOIN J2_TBL USING (|i)"
        );
        assertContains(proposals, "i", "j", "k");
        Assertions.assertTrue(proposals.stream().noneMatch(p -> p.contains(".")), proposals::toString);
    }

    @Test
    public void testPro3832NestedQueryColumns() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("t1", t -> {
                t.attribute("a");
                t.attribute("b");
                t.attribute("c");
            });
            s.table("t2", t -> {
                t.attribute("a");
                t.attribute("b");
                t.attribute("c");
            });
        }).prepare();

        Set<String> proposals = request.requestNewStrings("""
            SELECT | FROM
                (SELECT * FROM t1 WHERE a < 450) a1
                FULL JOIN
                (SELECT * FROM t2 WHERE b > 250) a2
                    ON a1.a = a2.b
            WHERE a1.b = 0 OR a2.a = 0
            ORDER BY a1.a, a2.b
            """);
        assertContains(proposals, "a1.a", "a1.b", "a1.c", "a2.a", "a2.b", "a2.c");
        Assertions.assertTrue(proposals.stream().noneMatch(p -> p.contains("a1.a1.") || p.contains("a2.a2.")),
            proposals::toString);
    }

    @Test
    public void testPro3839InsertColumns() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> s.table("inserttest", t -> {
            t.attribute("col1");
            t.attribute("col2");
            t.attribute("col3");
        })).prepare();

        Set<String> proposals = request.requestNewStrings(
            "insert into inserttest (|col1, col2, col3) values (1, DEFAULT, DEFAULT)"
        );
        assertContains(proposals, "col1", "col2", "col3");
        assertNotContains(proposals, "inserttest.col1", "inserttest.col2", "inserttest.col3");
    }

    @Test
    public void testPro3923CompletionInDerivedTableJoin() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("yy", t -> {
                t.attribute("pkyy");
                t.attribute("pkxx");
            });
            s.table("xx", t -> t.attribute("pkxx"));
        }).prepare();

        Set<String> proposals = request.requestNewStrings("""
            select yy.pkyy as yy_pkyy, yy.pkxx as yy_pkxx, yya.pkyy as yya_pkyy,
                   xxa.pkxx as xxa_pkxx, xxb.pkxx as xxb_pkxx
            from yy
                 left join (SELECT * FROM yy where pkyy = 101) as yya ON yy.pkyy = yya.|
                 left join xx xxa on yya.pkxx = xxa.pkxx
                 left join xx xxb on coalesce (xxa.pkxx, 1) = xxb.pkxx
            """);
        assertContains(proposals, "pkyy", "pkxx");
        assertNotContains(proposals, "yya.pkyy", "yya.pkxx", "yy.pkyy", "yy.pkxx");
    }

    @Test
    public void testPro4029TableCompletionImmediatelyAfterFrom() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("Artist", t -> {
                t.attribute("ArtistId");
                t.attribute("Name");
            });
            s.table("Album", t -> {
                t.attribute("AlbumId");
                t.attribute("Title");
            });
        }).prepare();

        Set<String> proposals = request.requestNewStrings("SELECT * FROM |");
        assertContains(proposals, "Artist a", "Album a");
        assertNotContains(proposals, "ArtistId", "AlbumId", "Name", "Title");
    }

    @Test
    public void testPro4058ColumnsAtJoinConditionTail() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("Artist", t -> {
                t.attribute("ArtistId");
                t.attribute("Name");
            });
            s.table("Album", t -> {
                t.attribute("AlbumId");
                t.attribute("Title");
                t.attribute("ArtistId");
            });
        }).prepare();

        Set<String> proposals = request.requestNewStrings("select * FROM Artist art join Album al ON |");
        assertContains(proposals, "art.ArtistId", "art.Name", "al.AlbumId", "al.Title", "al.ArtistId");
        assertNotContains(proposals, "Artist a", "Album a");
    }

    @Test
    public void testDbeaver36708And36887PreserveAliasCase() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("ACCESS$", t -> {
                t.attribute("D_OBJ#");
                t.attribute("ORDER#");
            });
            s.table("ROLE_TAB_PRIVS", t -> t.attribute("ROLE"));
        }).prepare();

        Set<String> proposals = request.requestNewStrings(
            "SELECT * FROM \"ACCESS$\" AS aCc JOIN ROLE_TAB_PRIVS rt ON true WHERE |"
        );
        assertContains(proposals, "aCc.\"D_OBJ#\"", "aCc.\"ORDER#\"", "rt.ROLE");
        Assertions.assertTrue(proposals.stream().noneMatch(p -> p.startsWith("ACC.") || p.startsWith("ACc.")),
            proposals::toString);
    }

    @Test
    public void testDbeaver36693CompletionAfterTableHint() throws DBException {
        RequestResult request = RequestBuilder.schemas(d -> d.schema("dbo", s -> s.table("Customers", t -> {
            t.attribute("id");
            t.attribute("Customer");
        }))).prepare();

        Set<String> proposals = request.requestNewStrings(
            "select c.id, c.Customer from dbo.Customers c with(nolock) where c.|"
        );
        assertContains(proposals, "id", "Customer");
    }

    @Test
    public void testDbeaver36582CompletionAfterSelectInto() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> s.table("film_actor", t -> {
            t.attribute("actor_id");
            t.attribute("film_id");
        })).prepare();

        Set<String> proposals = request.requestNewStrings(
            "select * into film_actor_copy from film_actor as fa where fa.|"
        );
        assertContains(proposals, "actor_id", "film_id");
    }

    @Test
    public void testDbeaver36573GroupByColumnsHaveNoAliases() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> s.table("tab1", t -> {
            t.attribute("id");
            t.attribute("statdate");
            t.attribute("enddate");
        })).prepare();

        Set<String> proposals = request.requestNewStrings("select * from tab1 as t group by |");
        assertContains(proposals, "id", "statdate", "enddate");
        Assertions.assertTrue(proposals.stream().noneMatch(p -> p.contains(" AS ")), proposals::toString);
    }

    @Test
    public void testDbeaver37074CorrelatedSubqueryScopes() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("test_tab_1", t -> {
                t.attribute("id");
                t.attribute("code");
                t.attribute("name");
                t.attribute("ext_id");
            });
            s.table("test_tab_2", t -> {
                t.attribute("id");
                t.attribute("code");
            });
        }).prepare();

        Set<String> inner = request.requestNewStrings("""
            select * from test_tab_1 t1
            where exists (select 1 from test_tab_2 t2 where t2.|)
            """);
        assertContains(inner, "id", "code");
        assertNotContains(inner, "name", "ext_id");

        Set<String> outer = request.requestNewStrings("""
            select * from test_tab_1 t1
            where exists (select 1 from test_tab_2 t2 where t2.id = t1.|)
            """);
        assertContains(outer, "id", "code", "name", "ext_id");
    }

    @Test
    public void testDbeaver34251SelfJoinAliases() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> s.table("test", t -> {
            t.attribute("id");
            t.attribute("value");
        })).prepare();

        assertContains(request.requestNewStrings("select t1.| from test t1 join test t2 on true"), "id", "value");
        assertContains(request.requestNewStrings("select t2.| from test t1 join test t2 on true"), "id", "value");
    }

    @Test
    public void testDbeaver37431CompletionAfterStraightJoin() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("sMarketplace_supply", t -> {
                t.attribute("id");
                t.attribute("mpWarehouseID");
                t.attribute("mpSupplyDateFact");
            });
            s.table("sPlace", t -> t.attribute("mpSupplyID"));
            s.table("sSerial", t -> t.attribute("placeID"));
        }).prepare();

        Set<String> proposals = request.requestNewStrings("""
            SELECT * FROM sMarketplace_supply AS mps
            STRAIGHT_JOIN sPlace AS pl ON pl.mpSupplyID = mps.id
            STRAIGHT_JOIN sSerial AS s ON s.placeID = pl.placeID
            WHERE mps.|
            """);
        assertContains(proposals, "id", "mpWarehouseID", "mpSupplyDateFact");
    }

    @Test
    public void testDbeaver36357LateralJoinScopes() throws DBException {
        RequestResult request = RequestBuilder.tables(s -> {
            s.table("pg_class", t -> {
                t.attribute("oid");
                t.attribute("relname");
            });
            s.table("pg_attribute", t -> {
                t.attribute("attrelid");
                t.attribute("attname");
            });
        }).prepare();

        Set<String> correlated = request.requestNewStrings("""
            select * from pg_class
            inner join lateral (
                select * from pg_attribute where attrelid = pg_class.|
            ) t on true
            """);
        assertContains(correlated, "oid", "relname");

        Set<String> projected = request.requestNewStrings("""
            select t.| from pg_class
            inner join lateral (
                select * from pg_attribute where attrelid = pg_class.oid
            ) t on true
            """);
        assertContains(projected, "attrelid", "attname");
    }

    @Test
    public void testDbeaver37476CompletionAtStatementTail() throws DBException {
        RequestResult request = RequestBuilder.schemas(d -> d.schema("clients", s -> s.table("tabl_clients", t -> {
            t.attribute("client_id");
            t.attribute("client_name");
            t.attribute("status");
        }))).prepare();

        Set<String> proposals = request.requestNewStrings(
            "SELECT * FROM clients.tabl_clients WHERE client_|"
        );
        assertContains(proposals, "client_id", "client_name");
    }

    private static void assertContains(@NotNull Set<String> actual, @NotNull String... expected) {
        for (String value : expected) {
            Assertions.assertTrue(actual.contains(value), () -> "Expected '" + value + "' in " + actual);
        }
    }

    private static void assertNotContains(@NotNull Set<String> actual, @NotNull String... unexpected) {
        for (String value : unexpected) {
            Assertions.assertFalse(actual.contains(value), () -> "Did not expect '" + value + "' in " + actual);
        }
    }
}
