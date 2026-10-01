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
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jkiss.dbeaver.model.sql.analyzer.builder.Builder.Consumer.empty;

public class SQLCompletionAnalyzerTest extends DBeaverUnitTest {
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
            final List<SQLCompletionProposalBase> proposals = request.request("SEL|");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("SELECT", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * |");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("FROM", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM T |");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("WHERE", proposals.get(0).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT | FROM Table1");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 WHERE |");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Table1.Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Table1.Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Table1.Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 WHERE Table1.|");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 t WHERE t.|");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM \"Table 3\" t WHERE t.|");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col7", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col8", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col9", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t.| FROM Table1 t");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t2.| FROM Table1 t, Table2 t2");

            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col4", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col5", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col6", proposals.get(2).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Table1 join Table2 t on t.|", false);
            Assertions.assertEquals("Col4", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col5", proposals.get(1).getReplacementString());
        }
        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Table1 b join Table2 on b.|", false);
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT *| FROM Table1", false);

            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Col1, Col2, Col3", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t.*| FROM Table1 t", false);

            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Col1, t.Col2, t.Col3", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT Table1.*| FROM Table1", false);

            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Col1, Table1.Col2, Table1.Col3", proposals.get(0).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM |");
            Assertions.assertTrue(proposals.size() >= 3);
            Assertions.assertEquals("Table1 t", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Table2 t", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Table3 t", proposals.get(2).getReplacementString());

            // TODO: Is 'WHERE' even supposed to be here?
            // Assertions.assertEquals("WHERE", proposals.get(3).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Tb|");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Tbl4 t", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Tbl5 t", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Tbl6 t", proposals.get(2).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Sch|");
            Assertions.assertEquals(2, proposals.size());
            Assertions.assertEquals("Schema1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Schema2", proposals.get(1).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Schema1.|");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Table1 t", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Table2 t", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Table3 t", proposals.get(2).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Da|");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Database1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Database2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Database3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database1.|");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Schema1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database1.Schema1.|");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Table1 t", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Table2 t", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Table3 t", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database3.|");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("\"a.schema\"", proposals.get(0).getReplacementString());
            Assertions.assertEquals(24, proposals.get(0).getReplacementOffset());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database3.\"a.schema\".|");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("\"a.table\" t", proposals.get(0).getReplacementString());
            Assertions.assertEquals(35, proposals.get(0).getReplacementOffset());
        }
    }

    @Test
    @Disabled("See #12159")
    public void testQuotedNamesCompletion() throws DBException {
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Dat|\"");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Database1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Sch|\"");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Schema1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Schema1\".\"Tab|\"");
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals("Table1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Schema1\".\"Table1\".\"Col|\"");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
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
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT | FROM Database1.Schema1.Table1");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT | FROM \"Database1\".Schema1.\"Table1\"");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT | FROM \"Database1\".\"Schema1\".\"Table1\"");
            Assertions.assertEquals(3, proposals.size());
            Assertions.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assertions.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assertions.assertEquals("Col3", proposals.get(2).getReplacementString());
        }
    }
    
    @Test
    public void testCompleteTablesWithAliasesPositive() throws DBException {
        List<SQLCompletionProposalBase> proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE |");
        
        Assertions.assertEquals("a.attribute1", proposals.get(0).getReplacementString());
        Assertions.assertEquals("a.attribute2", proposals.get(1).getReplacementString());
        Assertions.assertEquals("a.attribute3", proposals.get(2).getReplacementString());
        Assertions.assertEquals("b.attribute1", proposals.get(3).getReplacementString());
        Assertions.assertEquals("b.attribute2", proposals.get(4).getReplacementString());
        Assertions.assertEquals("b.attribute3", proposals.get(5).getReplacementString());

        proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE a.|");
        Assertions.assertEquals("attribute1", proposals.get(0).getReplacementString());
        Assertions.assertEquals("attribute2", proposals.get(1).getReplacementString());
        Assertions.assertEquals("attribute3", proposals.get(2).getReplacementString());

        proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE b.|");
        Assertions.assertEquals("attribute1", proposals.get(0).getReplacementString());
        Assertions.assertEquals("attribute2", proposals.get(1).getReplacementString());
        Assertions.assertEquals("attribute3", proposals.get(2).getReplacementString());
        
        proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND |");
        Assertions.assertEquals("a.attribute1", proposals.get(0).getReplacementString());
        Assertions.assertEquals("a.attribute2", proposals.get(1).getReplacementString());
        Assertions.assertEquals("a.attribute3", proposals.get(2).getReplacementString());
        Assertions.assertEquals("b.attribute1", proposals.get(3).getReplacementString());
        Assertions.assertEquals("b.attribute2", proposals.get(4).getReplacementString());
        Assertions.assertEquals("b.attribute3", proposals.get(5).getReplacementString());
        
        proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE a.attribute1=1 AND b.|");
        Assertions.assertEquals("attribute1", proposals.get(0).getReplacementString());
        Assertions.assertEquals("attribute2", proposals.get(1).getReplacementString());
        Assertions.assertEquals("attribute3", proposals.get(2).getReplacementString());

        // all
        proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        Assertions.assertEquals("a.\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("a.\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("a.\"attribute-Aa\"", proposals.get(2).getReplacementString());
        Assertions.assertEquals("b.\"attribute-a\"", proposals.get(3).getReplacementString());
        Assertions.assertEquals("b.\"attribute-A\"", proposals.get(4).getReplacementString());
        Assertions.assertEquals("b.\"attribute-Aa\"", proposals.get(5).getReplacementString());

        // a
        proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assertions.assertEquals("\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("\"attribute-Aa\"", proposals.get(2).getReplacementString());

        // b
        proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assertions.assertEquals("\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("\"attribute-Aa\"", proposals.get(2).getReplacementString());
    }
    
    @Test
    public void testCompleteTablesWithAliasesQuotedPositive() throws DBException {
        List<SQLCompletionProposalBase> proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE |");
        // alias from a and b
        Assertions.assertEquals("a.\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("a.\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("a.\"attribute-Aa\"", proposals.get(2).getReplacementString());
        Assertions.assertEquals("b.\"attribute-a\"", proposals.get(3).getReplacementString());
        Assertions.assertEquals("b.\"attribute-A\"", proposals.get(4).getReplacementString());
        Assertions.assertEquals("b.\"attribute-Aa\"", proposals.get(5).getReplacementString());
        // alias from a
        proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.|");
        Assertions.assertEquals("\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("\"attribute-Aa\"", proposals.get(2).getReplacementString());
        // alias from b
        proposals = modelDataRequest
            .request("SELECT * FROM tableNaMeA a, tableNaMeB b WHERE a.attribute-a=1 AND b.|");
        Assertions.assertEquals("\"attribute-a\"", proposals.get(0).getReplacementString());
        Assertions.assertEquals("\"attribute-A\"", proposals.get(1).getReplacementString());
        Assertions.assertEquals("\"attribute-Aa\"", proposals.get(2).getReplacementString());
    }

    @Test
    public void testCompleteTablesByAliaseNegative() throws DBException {
        List<SQLCompletionProposalBase> proposals = modelDataRequest
            .request("SELECT * FROM table1 a, table2 b WHERE c.|");
        Assertions.assertTrue(proposals.isEmpty());
    }
}
