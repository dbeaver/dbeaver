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

import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.Trie;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TrieLookupComparator;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class TrieTest extends DBeaverUnitTest {

    class TestToken {
        private String key;
        private Number number;

        public TestToken(String key, Number number) {
            this.key = key;
            this.number = number;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Number getNumber() {
            return number;
        }

        public void setNumber(Number number) {
            this.number = number;
        }
    }

    class TestLookupComparator implements TrieLookupComparator<String>, Comparator<String> {

        @Override
        public int compare(String first, String second) {
            if (first == null) {
                if (second == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (second == null) {
                    return 1;
                } else {
                    return getComparablePart(first).compareToIgnoreCase(getComparablePart(second));
                }
            }
        }

        @Override
        public boolean isStronglyComparable(String term) {
            return term != null && term.length() > 0 && term.length() < 3;
        }

        @Override
        public boolean isPartiallyComparable(String term) {
            return term != null && term.length() > 0;
        }

        private String getComparablePart(String value) {
            return value.substring(0, Math.min(value.length(), 2));
        }

        @Override
        public boolean match(String key, String term) {
            if (key == null || term == null || key.length() == 0 || term.length() == 0) {
                return true;
            } else {
                return getComparablePart(key).equalsIgnoreCase(getComparablePart(term));
            }
        }
    }

    /**
     * Build-up a trie of the following structure:
     * <pre>
     *               "5"
     *              /  \
     *             /    \
     *           "1"5)  "2"
     *          /   \      \
     *         /     \      \
     *       "2"4)   "3"    "3"
     *       / \       \      \
     *      /   \       \      \
     *    "3"   "4"6)   "4"3)  "4"2)
     *    /
     *   /
     * "4"1)
     * </pre>
     * Also augmented with some extra nodes at the level of "1"5) considered partially comparable
     * @return
     */
    private Trie<String, Integer> makeTrie() {
        Trie<String, Integer> t = new Trie<>(String::compareToIgnoreCase, new TestLookupComparator());

        t.add(List.of("5", "1", "2", "3", "4"), 1);
        t.add(List.of("5", "2", "3", "4"), 2);
        t.add(List.of("5", "1", "3", "4"), 3);
        t.add(List.of("5", "1", "2"), 4);
        t.add(List.of("5", "1"), 5);
        t.add(List.of("5", "1", "2", "4"), 6);
        t.add(List.of("5", "1xa", "2"), 7);
        t.add(List.of("5", "1yb", "2"), 8);
        t.add(List.of("5", "1yc", "2"), 9);
        t.add(List.of("5", "1yd", "2"), 10);
        t.add(List.of("5", "1ze", "2"), 11);
        t.add(List.of("5", "1y"), 12);
        t.add(Arrays.asList("5", "1", "2", "", "4"), 13);

        return t;
    }

    private List<Set<Integer>> makeExpectedResults() {
        return List.of(
                Set.of(1, 4, 5, 13),
                Set.of(2),
                Set.of(3, 5),
                Set.of(4, 5),
                Set.of(5),
                Set.of(4, 5, 6),
                Set.of(4, 5, 6, 7, 8, 9, 10, 11, 12),
                Set.of(8, 9, 10, 12),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    private List<List<String>> makeQueries() {
        return List.of(
                List.of("5", "1", "2", "3", "4"),
                List.of("5", "2", "3", "4"),
                List.of("5", "1", "3", "4"),
                List.of("5", "1", "2"),
                List.of("5", "1"),
                List.of("5", "1", "2", "4"),
                Arrays.asList("5", "", "2", "4"),
                List.of("5", "1y", "2"),
                Arrays.asList(new String[0]),
                List.of("6"),
                List.of("5", "2", "3", "10", "11")
        );
    }

    @Test
    public void trieLookup() {
        // build a trie
        Trie<String, Integer> t = makeTrie();

        // make a set of queries each resulting with a set of values
        List<Set<Integer>> results = makeQueries().stream()
                .map(p -> t.collectValuesOnPath(p.iterator()))
                .collect(Collectors.toList());

        // check out the expected query results for each corresponding query
        var expected = makeExpectedResults();

        Assert.assertEquals(expected, results);
    }

    @Test
    public void trieNonEmptyRootLookup() {
        // some extra values to augment trie' root with
        Set<Integer> rootValues = Set.of(-1, -2);

        // build a trie
        Trie<String, Integer> t = makeTrie();
        // augment its root with some extra values (trie root corresponds to the path of zero length)
        rootValues.forEach(v -> t.add(List.of(), v));

        // make a set of queries each resulting with a set of values
        List<Set<Integer>> results = makeQueries().stream()
                .map(p -> t.collectValuesOnPath(p.iterator()))
                .collect(Collectors.toList());

        var expected = makeExpectedResults().stream()
                .map(s -> new HashSet<>(s))
                .collect(Collectors.toList());
        // expected results are the same as in {@link #trieLookup()} test, but all of them will include root extra values now
        expected.forEach(s -> s.addAll(rootValues));

        Assert.assertEquals(expected, results);
    }
}
