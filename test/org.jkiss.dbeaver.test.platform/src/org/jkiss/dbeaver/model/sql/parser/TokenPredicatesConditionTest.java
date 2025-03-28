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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateNode;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenPredicatesConditionTest extends DBeaverUnitTest {

    private TokenPredicateNode makeTestPredicateTree() {
        TokenPredicateFactory tt = TokenPredicateFactory.makeDefaultFactory();

        TokenPredicateNode predicate = tt.sequence(
            "CREATE",
            tt.optional("OR", "REPLACE"),
            tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
            "PACKAGE", "BODY"
        );

        return predicate;
    }

    private Set<List<String>> makeTestPredicateSequences() {
        return Set.of(
                List.of("CREATE", "PACKAGE", "BODY"),
                List.of("CREATE", "OR", "REPLACE", "PACKAGE", "BODY"),
                List.of("CREATE", "EDITIONABLE", "PACKAGE", "BODY"),
                List.of("CREATE", "OR", "REPLACE", "EDITIONABLE", "PACKAGE", "BODY"),
                List.of("CREATE", "NONEDITIONABLE", "PACKAGE", "BODY"),
                List.of("CREATE", "OR", "REPLACE", "NONEDITIONABLE", "PACKAGE", "BODY")
        );
    }

    @Test
    public void expandExpression() {
        // build a predicate tree describing some sequence of tokens
        TokenPredicateNode tree = this.makeTestPredicateTree();

        // expected path sequences to be described by given predicate
        Set<List<String>> expectedSequences = this.makeTestPredicateSequences();

        // expand the tree into a set of plain token sequences and map token entries to strings
        Set<List<String>> computedSequences = tree.expand().stream().map(
                seq -> seq.stream().map(t -> t.getString()).collect(Collectors.toList())
        ).collect(Collectors.toSet());

        Assert.assertEquals(expectedSequences, computedSequences);
    }
}
