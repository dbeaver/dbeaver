/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A set of information about blocks for autoedit strategy.
 */
public class SQLBlockCompletionsCollection implements SQLBlockCompletions {
    
    private static final Predicate<String> RECOGNIZABLE_TOKEN_PATTERN = Pattern.compile("^\\w+$").asMatchPredicate();
    
    private final Map<String, Integer> tokenIdByString = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<String> tokenStringById = new ArrayList<>();   
    
    private final Map<Integer, SQLBlockCompletionInfo> blockCompletionByHeadToken = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<SQLBlockCompletionInfo>>> blockCompletionByTailToken = new HashMap<>();
    
    /**
     * Get SQLBlockCompletionInfo by block beginning token id.
     */
    @Nullable
    public SQLBlockCompletionInfo findCompletionByHead(int headTokenId) {
        return blockCompletionByHeadToken.get(headTokenId);
    }

    @Nullable
    public String findTokenString(int id) {
        return tokenStringById.get(id - KNOWN_TOKEN_ID_BASE);
    }

    @Nullable
    public Integer findTokenId(@NotNull String str) {
        return tokenIdByString.get(str);
    }
    
    /**
     * Get token id for token string.
     * If token has't been registered yet, id will be generated.
     */
    private int obtainTokenId(@NotNull String str) {
        if (!RECOGNIZABLE_TOKEN_PATTERN.test(str)) {
            throw new IllegalArgumentException("Illegal block completion part '" + str + "' while expecting keyword-like token.");
        }
        
        Integer id = tokenIdByString.get(str);
        if (id == null) {
            id = tokenStringById.size() + KNOWN_TOKEN_ID_BASE;
            tokenStringById.add(str);
            tokenIdByString.put(str, id);
        }
        return id;
    }

    /**
     * Register block for autoedit containing token at the begin and token at the end (e.g. BEGIN .. END).
     * @param headToken is a beginning token of the block
     * @param tailToken is an ending token of the block
     */
    public void registerCompletionPair(@NotNull String headToken, @NotNull String tailToken) {
        this.registerBlockCompletionInfo(headToken, new String[] {
            NEW_LINE_COMPLETION_PART, ONE_INDENT_COMPLETION_PART, NEW_LINE_COMPLETION_PART, tailToken, NEW_LINE_COMPLETION_PART
        }, tailToken, null, null);
    }

    /**
     * Register block for autoedit containing token at the begin and two tokens at the end (e.g. LOOP .. END LOOP).
     * @param headToken is a beginning token of the block.
     * @param tailToken is a  beginning token of the block end.
     * @param tailEndToken - last token of the block.
     */
    public void registerCompletionPair(@NotNull String headToken, @NotNull String tailToken, @NotNull String tailEndToken) {
        this.registerCompletionInfo(headToken, new String[] {
            NEW_LINE_COMPLETION_PART, ONE_INDENT_COMPLETION_PART, NEW_LINE_COMPLETION_PART, tailToken + " " + tailEndToken, NEW_LINE_COMPLETION_PART
        }, tailToken, tailEndToken);
    }
    
    /**
     * Register block for autoedit containing token at the begin, middle token and one or two tokens at the end (e.g. IF .. THEN .. END IF).
     * @param headToken is a beginning token of the block.
     * @param completionParts is an array of strings which should be inserted on autoedit.
     * Completion part can be a String,
     * SQLBlockCompletions.ONE_INDENT_COMPLETION_PART - indentation,
     * SQLBlockCompletions.NEW_LINE_COMPLETION_PART - new line.
     * @param tailToken - first token of the block end.
     * @param tailEndToken - last token of the block end.
     */
    public void registerCompletionInfo(@NotNull String headToken, @NotNull String[] completionParts,
                                       @NotNull String tailToken, @Nullable String tailEndToken) {
        this.registerBlockCompletionInfo(headToken, completionParts, tailToken, tailEndToken, headToken.equalsIgnoreCase(tailEndToken) ? tailToken : null);
    }
    
    
    private void registerBlockCompletionInfo(@NotNull String headToken, @NotNull String[] completionParts,
                                             @NotNull String tailToken, @Nullable String tailEndToken, @Nullable String prevCancelToken) {
        if (headToken == null || completionParts == null || tailToken == null) {
            throw new IllegalArgumentException("Illegal block completion info. headToken, completionParts and tailToken are mandatory.");
        }
        
        SQLBlockCompletionInfo info = new SQLBlockCompletionInfo(
                this,
                obtainTokenId(headToken), 
                completionParts, 
                obtainTokenId(tailToken),
                tailEndToken == null ? null : obtainTokenId(tailEndToken),
                // token that shouldn't precede  the block begin token (example: END for block LOOP .. END LOOP)
                prevCancelToken == null ? null : obtainTokenId(prevCancelToken) 
        );
        this.blockCompletionByHeadToken.put(info.getHeadTokenId(), info);
        this.blockCompletionByTailToken.computeIfAbsent(info.getTailTokenId(), n -> new HashMap<>())
           .computeIfAbsent(info.getTailEndTokenId(), n -> new HashSet<>())
           .add(info);
    }
}

