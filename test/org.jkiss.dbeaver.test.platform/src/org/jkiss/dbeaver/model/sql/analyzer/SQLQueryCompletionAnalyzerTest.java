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
}
