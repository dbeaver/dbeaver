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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    public SQLBlockCompletionInfo findCompletionByHead(int headTokenId) {
        return blockCompletionByHeadToken.get(headTokenId);
    }
    
    public String getTokenString(int id) {
        String tokenString = tokenStringById.get(id - KNOWN_TOKEN_ID_BASE);
        if (tokenString == null) {
            throw new IllegalArgumentException("Unknown token id " + id);
        } else {
            return tokenString;   
        }        
    }
    
    public Integer findTokenId(String str) {
        return tokenIdByString.get(str);
    }
    
    /**
     * Get token id for token string.
     * If token has't been registered yet, id will be generated.
     */
    private int obtainTokenId(String str) {
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
    public void registerCompletionPair(String headToken, String tailToken) {
        this.registerBlockCompletionInfo(headToken, new String[] { null, ONE_INDENT_COMPLETION_PART, null, tailToken, null }, tailToken, null, null);
    }

    /**
     * Register block for autoedit containing token at the begin and two tokens at the end (e.g. LOOP .. END LOOP).
     * @param headToken is a beginning token of the block.
     * @param tailToken is a  beginning token of the block end.
     * @param tailEndToken - last token of the block.
     */
    public void registerCompletionPair(String headToken, String tailToken, String tailEndToken) {
        this.registerCompletionInfo(headToken, new String[] { null, tailToken + " " + tailEndToken, null }, tailToken, tailEndToken);
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
    public void registerCompletionInfo(String headToken, String[] completionParts, String tailToken, String tailEndToken) {
        this.registerBlockCompletionInfo(headToken, completionParts, tailToken, tailEndToken, tailEndToken.equalsIgnoreCase(headToken) ? tailToken : null);
    }
    
    
    private void registerBlockCompletionInfo(String headToken, String[] completionParts, String tailToken, String tailEndToken, String prevCancelToken) {
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
        this.blockCompletionByHeadToken.put(info.headTokenId, info);    
        this.blockCompletionByTailToken.computeIfAbsent(info.tailTokenId, n -> new HashMap<>())
           .computeIfAbsent(info.tailEndTokenId, n -> new HashSet<>())
           .add(info);
    }
}

