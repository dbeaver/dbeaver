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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.model.ai.impl.MessageChunk;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AIMarkdownTest extends DBeaverUnitTest {

    @Test
    public void mdEmptyText() {
        String md = """
            """;
        MessageChunk[] messageChunks = AITextUtils.splitIntoChunks(md, true);
        assertEquals(messageChunks.length, 0);
    }

    @Test
    public void mdMixedText() {
        String md = """
            This is some query
            ```
            select 1
              ```
            And trailing footer
            """;
        MessageChunk[] messageChunks = AITextUtils.splitIntoChunks(md, true);
        assertEquals(messageChunks.length, 3);
        assertEquals("```\nselect 1\n```", messageChunks[1].toRawString());
    }

    @Test
    public void mdMultiQueries() {
        String md = """
            Query1: ```sql
            select 1
              ```
            Query 2:
            ```
            select 2
            ```
              Query 3:
              ```sql
                select 3;
                ```
            Query 4:
             ```
             select 4;
            ```
            Query 5:
             ```
            select 5
             union all
            select 6
            ```
              and some footer
            """;
        MessageChunk[] messageChunks = AITextUtils.splitIntoChunks(md, true);
        assertEquals(messageChunks.length, 11);
        assertTrue(messageChunks[1] instanceof MessageChunk.Code code && "select 1".equals(code.text()));
        assertTrue(messageChunks[3] instanceof MessageChunk.Code code && "select 2".equals(code.text()));
        assertTrue(messageChunks[5] instanceof MessageChunk.Code code && "    select 3;".equals(code.text()));
        assertTrue(messageChunks[7] instanceof MessageChunk.Code code && " select 4;".equals(code.text()));
        assertEquals(messageChunks[8].toRawString(), "Query 5:");
        assertTrue(messageChunks[9] instanceof MessageChunk.Code code && "select 5\n union all\nselect 6".equals(code.text()));
    }


}
