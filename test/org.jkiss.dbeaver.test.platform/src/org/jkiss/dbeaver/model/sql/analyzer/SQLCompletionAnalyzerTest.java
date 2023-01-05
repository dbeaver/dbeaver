/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestResult;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.jkiss.dbeaver.model.sql.analyzer.builder.Builder.Consumer.empty;

public class SQLCompletionAnalyzerTest {
    @Test
    public void testKeywordCompletion() throws DBException {
        final RequestResult request = RequestBuilder
            .empty()
            .prepare();

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SEL|");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("SELECT", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * |");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("FROM", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM T |");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("WHERE", proposals.get(0).getReplacementString());
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

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 WHERE |");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 WHERE Table1.|");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM Table1 t WHERE t.|");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT * FROM \"Table 3\" t WHERE t.|");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col7", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col8", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col9", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t.| FROM Table1 t");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t2.| FROM Table1 t, Table2 t2");

            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col4", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col5", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col6", proposals.get(2).getReplacementString());
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
            Assert.assertEquals("Col4", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col5", proposals.get(1).getReplacementString());
        }
        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Table1 b join Table2 on b.|", false);
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
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

            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Col1, Col2, Col3", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT t.*| FROM Table1 t", false);

            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Col1, t.Col2, t.Col3", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request
                .request("SELECT Table1.*| FROM Table1", false);

            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Col1, Table1.Col2, Table1.Col3", proposals.get(0).getReplacementString());
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
            Assert.assertTrue(proposals.size() >= 3);
            Assert.assertEquals("Table1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Table2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Table3", proposals.get(2).getReplacementString());

            // TODO: Is 'WHERE' even supposed to be here?
            // Assert.assertEquals("WHERE", proposals.get(3).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Tb|");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Tbl4", proposals.get(0).getReplacementString());
            Assert.assertEquals("Tbl5", proposals.get(1).getReplacementString());
            Assert.assertEquals("Tbl6", proposals.get(2).getReplacementString());
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
            Assert.assertEquals(2, proposals.size());
            Assert.assertEquals("Schema1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Schema2", proposals.get(1).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Schema1.|");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Table1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Table2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Table3", proposals.get(2).getReplacementString());
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
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Database1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Database2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Database3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database1.|");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Schema1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database1.Schema1.|");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Table1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Table2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Table3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database3.|");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("\"a.schema\"", proposals.get(0).getReplacementString());
            Assert.assertEquals(24, proposals.get(0).getReplacementOffset());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM Database3.\"a.schema\".|");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("\"a.table\"", proposals.get(0).getReplacementString());
            Assert.assertEquals(35, proposals.get(0).getReplacementOffset());
        }
    }

    @Test
    @Ignore("See #12159")
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
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Database1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Sch|\"");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Schema1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Schema1\".\"Tab|\"");
            Assert.assertEquals(1, proposals.size());
            Assert.assertEquals("Table1", proposals.get(0).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT * FROM \"Database1\".\"Schema1\".\"Table1\".\"Col|\"");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
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
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT | FROM \"Database1\".Schema1.\"Table1\"");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }

        {
            final List<SQLCompletionProposalBase> proposals = request.request("SELECT | FROM \"Database1\".\"Schema1\".\"Table1\"");
            Assert.assertEquals(3, proposals.size());
            Assert.assertEquals("Col1", proposals.get(0).getReplacementString());
            Assert.assertEquals("Col2", proposals.get(1).getReplacementString());
            Assert.assertEquals("Col3", proposals.get(2).getReplacementString());
        }
    }
}
