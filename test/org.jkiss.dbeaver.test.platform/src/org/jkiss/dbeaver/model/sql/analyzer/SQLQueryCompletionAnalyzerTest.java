/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.junit.osgi.annotation.RunnerProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

import static org.jkiss.dbeaver.model.sql.analyzer.builder.Builder.Consumer.empty;

@RunnerProxy(MockitoJUnitRunner.Silent.class)
public class SQLQueryCompletionAnalyzerTest extends DBeaverUnitTest {
    private static RequestResult modelDataRequest;

    @Before
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
            
            Assert.assertTrue(proposals.contains("SELECT"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * |");
            
            Assert.assertTrue(proposals.contains("FROM"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM T |");
            
            Assert.assertTrue(proposals.contains("WHERE"));
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


            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 WHERE |");

            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 WHERE Table1.|");


            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM Table1 t WHERE t.|");

            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT * FROM \"Table 3\" t WHERE t.|");

            
            Assert.assertTrue(proposals.contains("Col7"));
            Assert.assertTrue(proposals.contains("Col8"));
            Assert.assertTrue(proposals.contains("Col9"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t.| FROM Table1 t");

            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t2.| FROM Table1 t, Table2 t2");

            
            Assert.assertTrue(proposals.contains("Col4"));
            Assert.assertTrue(proposals.contains("Col5"));
            Assert.assertTrue(proposals.contains("Col6"));
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
            Assert.assertTrue(proposals.contains("Col4"));
            Assert.assertTrue(proposals.contains("Col5"));
        }
        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Table1 b join Table2 on b.|", false);
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
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

            
            Assert.assertTrue(proposals.contains("Col1, Col2, Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT t.*| FROM Table1 t", false);

            
            Assert.assertTrue(proposals.contains("t.Col1, t.Col2, t.Col3"));
        }

        {
            final Set<String> proposals = request
                .requestNewStrings("SELECT Table1.*| FROM Table1", false);

            
            Assert.assertTrue(proposals.contains("Col1, Col2, Col3"));
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
            Assert.assertTrue(proposals.size() >= 3);
            Assert.assertTrue(proposals.contains("Table1 t"));
            Assert.assertTrue(proposals.contains("Table2 t"));
            Assert.assertTrue(proposals.contains("Table3 t"));
            Assert.assertTrue(proposals.contains("Tbl4 t"));
            Assert.assertTrue(proposals.contains("Tbl5 t"));
            Assert.assertTrue(proposals.contains("Tbl6 t"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Tb|");
            Assert.assertFalse(proposals.contains("Table1 t"));
            Assert.assertFalse(proposals.contains("Table2 t"));
            Assert.assertFalse(proposals.contains("Table3 t"));
            Assert.assertTrue(proposals.contains("Tbl4 t"));
            Assert.assertTrue(proposals.contains("Tbl5 t"));
            Assert.assertTrue(proposals.contains("Tbl6 t"));
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
            
            Assert.assertTrue(proposals.contains("Schema1"));
            Assert.assertTrue(proposals.contains("Schema2"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Schema1.|");
            
            Assert.assertTrue(proposals.contains("Table1 t"));
            Assert.assertTrue(proposals.contains("Table2 t"));
            Assert.assertTrue(proposals.contains("Table3 t"));
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
            
            Assert.assertTrue(proposals.contains("Database1"));
            Assert.assertTrue(proposals.contains("Database2"));
            Assert.assertTrue(proposals.contains("Database3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database1.|");
            
            Assert.assertTrue(proposals.contains("Schema1"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database1.Schema1.|");
            
            Assert.assertTrue(proposals.contains("Table1 t"));
            Assert.assertTrue(proposals.contains("Table2 t"));
            Assert.assertTrue(proposals.contains("Table3 t"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database3.|");
            
            Assert.assertTrue(proposals.contains("\"a.schema\""));
            
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT * FROM Database3.\"a.schema\".|");
            
            Assert.assertTrue(proposals.contains("\"a.table\" t"));
            
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
            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT | FROM \"Database1\".Schema1.\"Table1\"");
            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
        }

        {
            final Set<String> proposals = request.requestNewStrings("SELECT | FROM \"Database1\".\"Schema1\".\"Table1\"");
            
            Assert.assertTrue(proposals.contains("Col1"));
            Assert.assertTrue(proposals.contains("Col2"));
            Assert.assertTrue(proposals.contains("Col3"));
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
            Assert.assertTrue(proposals.contains("col1"));
            Assert.assertTrue(proposals.contains("col2"));
            Assert.assertTrue(proposals.contains("col3"));
        }
    }
    
    @Test
    public void testCompleteTablesWithAliasesPositive() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE |");
        
        Assert.assertTrue(proposals.contains("a.attribute1"));
        Assert.assertTrue(proposals.contains("a.attribute2"));
        Assert.assertTrue(proposals.contains("a.attribute3"));
        Assert.assertTrue(proposals.contains("b.attribute1"));
        Assert.assertTrue(proposals.contains("b.attribute2"));
        Assert.assertTrue(proposals.contains("b.attribute3"));

        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.|");
        Assert.assertTrue(proposals.contains("attribute1"));
        Assert.assertTrue(proposals.contains("attribute2"));
        Assert.assertTrue(proposals.contains("attribute3"));

        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE b.|");
        Assert.assertTrue(proposals.contains("attribute1"));
        Assert.assertTrue(proposals.contains("attribute2"));
        Assert.assertTrue(proposals.contains("attribute3"));
        
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND |");
        Assert.assertTrue(proposals.contains("a.attribute1"));
        Assert.assertTrue(proposals.contains("a.attribute2"));
        Assert.assertTrue(proposals.contains("a.attribute3"));
        Assert.assertTrue(proposals.contains("b.attribute1"));
        Assert.assertTrue(proposals.contains("b.attribute2"));
        Assert.assertTrue(proposals.contains("b.attribute3"));
        
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND b.|");
        Assert.assertTrue(proposals.contains("attribute1"));
        Assert.assertTrue(proposals.contains("attribute2"));
        Assert.assertTrue(proposals.contains("attribute3"));

        // all
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        Assert.assertTrue(proposals.contains("a.\"attribute-a\""));
        Assert.assertTrue(proposals.contains("a.\"attribute-A\""));
        Assert.assertTrue(proposals.contains("a.\"attribute-Aa\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-a\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-A\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-Aa\""));

        // a
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assert.assertTrue(proposals.contains("\"attribute-a\""));
        Assert.assertTrue(proposals.contains("\"attribute-A\""));
        Assert.assertTrue(proposals.contains("\"attribute-Aa\""));

        // b
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assert.assertTrue(proposals.contains("\"attribute-a\""));
        Assert.assertTrue(proposals.contains("\"attribute-A\""));
        Assert.assertTrue(proposals.contains("\"attribute-Aa\""));
    }
    
    @Test
    public void testCompleteTablesWithAliasesQuotedPositive() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        // alias from a and b
        Assert.assertTrue(proposals.contains("a.\"attribute-a\""));
        Assert.assertTrue(proposals.contains("a.\"attribute-A\""));
        Assert.assertTrue(proposals.contains("a.\"attribute-Aa\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-a\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-A\""));
        Assert.assertTrue(proposals.contains("b.\"attribute-Aa\""));
        // alias from a
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assert.assertTrue(proposals.contains("\"attribute-a\""));
        Assert.assertTrue(proposals.contains("\"attribute-A\""));
        Assert.assertTrue(proposals.contains("\"attribute-Aa\""));
        // alias from b
        proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assert.assertTrue(proposals.contains("\"attribute-a\""));
        Assert.assertTrue(proposals.contains("\"attribute-A\""));
        Assert.assertTrue(proposals.contains("\"attribute-Aa\""));
    }

    @Test
    public void testCompleteTablesByAliaseNegative() throws DBException {
        Set<String> proposals = modelDataRequest
            .requestNewStrings("SELECT * FROM table1 a, table2 b WHERE c.|");
        Assert.assertTrue(proposals.isEmpty());
    }
}
