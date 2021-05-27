/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SQLCompletionAnalyzerTest {
    @Test
    public void testCompletionKeywordSelect() throws DBException {
        final List<SQLCompletionProposalBase> proposals = new SQLCompletionRequestBuilder()
            .request("SEL|");

        Assert.assertEquals(1, proposals.size());
        Assert.assertEquals("SELECT", proposals.get(0).getReplacementString());
    }

    @Test
    public void testCompletionTableAfterSelect() throws DBException {
        final List<SQLCompletionProposalBase> proposals = new SQLCompletionRequestBuilder()
            .addTable("A").build()
            .addTable("B").build()
            .addTable("C").build()
            .request("SELECT * FROM |");

        Assert.assertEquals(4, proposals.size());
        Assert.assertEquals("A", proposals.get(0).getReplacementString());
        Assert.assertEquals("B", proposals.get(1).getReplacementString());
        Assert.assertEquals("C", proposals.get(2).getReplacementString());

        // TODO: Is 'WHERE' even supposed to be here?
        Assert.assertEquals("WHERE", proposals.get(3).getReplacementString());
    }

    @Test
    public void testCompletionTablePartialAfterSelect() throws DBException {
        final List<SQLCompletionProposalBase> proposals = new SQLCompletionRequestBuilder()
            .addTable("A1").build()
            .addTable("A2").build()
            .addTable("B1").build()
            .addTable("B2").build()
            .request("SELECT * FROM A|");

        Assert.assertEquals(2, proposals.size());
        Assert.assertEquals("A1", proposals.get(0).getReplacementString());
        Assert.assertEquals("A2", proposals.get(1).getReplacementString());
    }

    @Test
    public void testCompletionColumnAfterSelectFromTable() throws DBException {
        final List<SQLCompletionProposalBase> proposals = new SQLCompletionRequestBuilder()
            .addTable("A")
                .addAttribute("col1")
                .addAttribute("col2")
                .addAttribute("col3")
                .build()
            .request("SELECT * FROM A.|");

        Assert.assertEquals(3, proposals.size());
        Assert.assertEquals("col1", proposals.get(0).getReplacementString());
        Assert.assertEquals("col2", proposals.get(1).getReplacementString());
        Assert.assertEquals("col3", proposals.get(2).getReplacementString());
    }

    @Test
    public void testExpandAllColumnsTable() throws DBException {
        final List<SQLCompletionProposalBase> proposals = new SQLCompletionRequestBuilder()
            .addTable("A")
                .addAttribute("col1")
                .addAttribute("col2")
                .addAttribute("col3")
                .build()
            .request("SELECT *| FROM A");

        Assert.assertEquals(1, proposals.size());
        Assert.assertEquals("col1, col2, col3", proposals.get(0).getReplacementString());
    }
}
