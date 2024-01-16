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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implements token predicate tree expansion into the list of all possible token entry sequences matching the predicate
 */
class TokenPredicateExpander implements TokenPredicateNodeVisitor<ListNode<SQLTokenEntry>, ListNode<ListNode<SQLTokenEntry>>> {
    private static final TokenPredicateExpander INSTANCE = new TokenPredicateExpander();

    @NotNull
    public static List<List<TokenEntry>> expand(@Nullable TokenPredicateNode node) {
        return node == null
                ? Collections.emptyList()
                : StreamSupport.stream(node.apply(INSTANCE, null).spliterator(), false)
                .filter(p -> p != null)
                .map(path -> {
                    // expanding traverse walks in the left-to-right order, so we've got the rightmost entry  as head of the list
                    List<TokenEntry> list = StreamSupport.stream(path.spliterator(), false).collect(Collectors.toList());
                    Collections.reverse(list); //can be removed if collect token entries in reverse order (right-to-left)
                    return list;
                })
                .collect(Collectors.toList());
    }

    private TokenPredicateExpander() {

    }

    @Override
    @NotNull
    public ListNode<ListNode<SQLTokenEntry>> visitSequence(@NotNull SequenceTokenPredicateNode sequence, @NotNull ListNode<SQLTokenEntry> head) {
        ListNode<ListNode<SQLTokenEntry>> results = ListNode.of(head);
        for (TokenPredicateNode child: sequence.childs) {
            ListNode<ListNode<SQLTokenEntry>> step = null;
            for (ListNode<SQLTokenEntry> prefix: results) {
                for (ListNode<SQLTokenEntry> childPath: child.apply(this, prefix)) {
                    step = ListNode.push(step, childPath);
                }
            }
            results = step;
        }
        return results;
    }

    @Override
    @NotNull
    public ListNode<ListNode<SQLTokenEntry>> visitAlternative(@NotNull AlternativeTokenPredicateNode alternative, @NotNull ListNode<SQLTokenEntry> head) {
        ListNode<ListNode<SQLTokenEntry>> results = null;
        for (TokenPredicateNode child: alternative.childs) {
            for (ListNode<SQLTokenEntry> childPath: child.apply(this, head)) {
                results = ListNode.push(results, childPath);
            }
        }
        return results;
    }

    @Override
    @NotNull
    public ListNode<ListNode<SQLTokenEntry>> visitOptional(@NotNull OptionalTokenPredicateNode optional, @NotNull ListNode<SQLTokenEntry> head) {
        return ListNode.push(optional.child.apply(this, head), head);
    }

    @Override
    @NotNull
    public ListNode<ListNode<SQLTokenEntry>> visitTokenEntry(@NotNull SQLTokenEntry token, @NotNull ListNode<SQLTokenEntry> head) {
        return ListNode.of(ListNode.push(head, token));
    }
}
