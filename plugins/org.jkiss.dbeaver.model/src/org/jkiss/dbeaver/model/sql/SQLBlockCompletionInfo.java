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
package org.jkiss.dbeaver.model.sql;

public class SQLBlockCompletionInfo {
    private final SQLBlockCompletionsCollection owner;

    public final int headTokenId;
    public final String[] completionParts;
    public final int tailTokenId;
    public final Integer tailEndTokenId;
    public final Integer headCancelTokenId;

    /**
     * @param owner - SQLBlockCompletionsCollection where SQLBlockCompletionInfo is registered
     * @param headTokenId - id of the beginning token of the block.
     * @param completionParts - array of strings which should be inserted on autoedit
     * Completion part can be a String, SQLBlockCompletions.ONE_INDENT_COMPLETION_PART - indentation, SQLBlockCompletions.NEW_LINE_COMPLETION_PART - new line.
     * @param tailTokenId - id of the first token of the block end
     * @param tailEndTokenId - id of the last token of the block end
     * @param prevCancelTokenId - token that shouldn't precede the block begin token
     */
    public SQLBlockCompletionInfo(SQLBlockCompletionsCollection owner, int headTokenId, String[] completionParts, int tailTokenId, Integer tailEndTokenId, Integer prevCancelTokenId) {
        this.owner = owner;
        this.headTokenId = headTokenId;
        this.completionParts = completionParts;
        this.tailTokenId = tailTokenId;
        this.tailEndTokenId = tailEndTokenId;
        this.headCancelTokenId = prevCancelTokenId;
    }

    private String getTokenString(Integer tokenId) {
        return tokenId == null ? "<UNBOUND>" : owner.getTokenString((int)tokenId);
    }

    @Override
    public String toString() {
        return (headCancelTokenId == null ? "" : ("[! " + getTokenString(headCancelTokenId) + "]")) +
                getTokenString(headTokenId) + " ... " + getTokenString(tailTokenId) + " " + getTokenString(tailEndTokenId);
    }
}

