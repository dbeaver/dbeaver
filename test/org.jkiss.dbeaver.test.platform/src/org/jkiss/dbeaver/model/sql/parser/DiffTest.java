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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.Diff;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.IntStream;

public class DiffTest  extends DBeaverUnitTest {

    List<Pair<String, List<String>>> testCases = List.of(
        // insertions
        Pair.of(
            "abcdefghijklmnopqrstuvwxyz",
            List.of(
                "XYZabcdefghijklmnopqrstuvwxyz",
                "abcdefghijklmnopqrstuvwxyzXYZ",
                "abcdefghijklmnopXYZqrstuvwxyz",
                "abcdefgXYZhijklmnopqrstuvwxyz",
                "abcdefgXYZhijklmnopUVWqrstuvwxyz",
                "abcdefghijklmXYZnUVWopqrstuvwxyz",
                "123abcdefgXYZhijklmnopqrstuvwxyz",
                "123aXYZbcdefghijklmnopqrstuvwxyz",
                "abcdefgXYZhijklmnopqrstuvwxyz456",
                "abcdefghijklmnopqrstuvwxyXYZz456",
                "123abcdefgXYZhijklmnopqrstuvwxyz456",
                "123abcdefgXYZhijklmnopUVWqrstuvwxyz456",
                "123aXYZbcdefghijklmnopqrstuvwxyUVWz456"
            )
        ),
        // removals
        Pair.of(
            "123abcdefgXYZhijklmnopUVWqrstuvwxyz456",
            List.of(
                "abcdefgXYZhijklmnopUVWqrstuvwxyz456",
                "123abcdefgXYZhijklmnopUVWqrstuvwxyz",
                "123abcdefghijklmnopUVWqrstuvwxyz456",
                "123abcdefgXYZhijklmnopqrstuvwxyz456",
                "abcdefgXYZhijklmnopqrstuvwxyz456",
                "123abcdefghijklmnopUVWqrstuvwxyz",
                "abcdefgXYZhijklmnopUVWqrstuvwxyz",
                "123abcdefghijklmnopqrstuvwxyz456",
                "abcdefgXYZhijklmnopUVWqrstuvwxyz",
                "abcdefghijklmnopqrstuvwxyz"
            )
        ),
        Pair.of(
            "123aXYZbcdefghijklmnopqrstuvwxyUVWz456",
            List.of(
                "aXYZbcdefghijklmnopqrstuvwxyUVWz456",
                "123aXYZbcdefghijklmnopqrstuvwxyUVWz",
                "123abcdefghijklmnopqrstuvwxyUVWz456",
                "123aXYZbcdefghijklmnopqrstuvwxyz456",
                "aXYZbcdefghijklmnopqrstuvwxyz456",
                "123abcdefghijklmnopqrstuvwxyUVWz",
                "aXYZbcdefghijklmnopqrstuvwxyUVWz",
                "123abcdefghijklmnopqrstuvwxyz456",
                "aXYZbcdefghijklmnopqrstuvwxyUVWz",
                "abcdefghijklmnopqrstuvwxyz"
            )
        )
    );

    @NotNull
    private <T> T[] applyForward(@NotNull T[] a, @NotNull T[] b, @NotNull List<Diff.Range> diff) {
        List<T> x = new LinkedList<>(Arrays.asList(a));

        ListIterator<T> it = x.listIterator();
        for (Diff.Range r : diff) {
            switch (r.operation) {
                case MATCH_AB -> IntStream.range(0, r.length).forEach(n -> it.next());
                case DELETE_A -> IntStream.range(0, r.length).forEach(n -> {
                    it.next();
                    it.remove();
                });
                case INSERT_B -> IntStream.range(0, r.length).forEach(n -> it.add(b[r.start + n]));
                default -> throw new AssertionError("Unknown operation " + r.operation);
            }
        }

        if (it.hasNext()) {
            throw new AssertionError("End of sequence expected but not reached at " + x.subList(it.nextIndex(), x.size() - 1));
        }

        //noinspection unchecked
        return x.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), x.size()));
    }

    @NotNull
    private <T> T[] applyBackward(@NotNull T[] a, @NotNull T[] b, @NotNull List<Diff.Range> diff) {
        List<T> y = new LinkedList<>(Arrays.asList(b));

        ListIterator<T> it = y.listIterator();
        for (Diff.Range r : diff) {
            switch (r.operation) {
                case MATCH_AB -> IntStream.range(0, r.length).forEach(n -> it.next());
                case DELETE_A -> IntStream.range(0, r.length).forEach(n -> it.add(a[r.start + n]));
                case INSERT_B -> IntStream.range(0, r.length).forEach(n -> {
                    it.next();
                    it.remove();
                });
                default -> throw new AssertionError("Unknown operation " + r.operation);
            }
        }

        if (it.hasNext()) {
            throw new AssertionError("End of sequence expected but not reached at " + y.subList(it.nextIndex(), y.size() - 1));
        }

        //noinspection unchecked
        return y.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), y.size()));
    }

    @NotNull
    private <T> T[] applyMatchAndInsertForB(@NotNull T[] a, @NotNull T[] b, @NotNull List<Diff.Range> diff) {
        List<T> result = new LinkedList<>();
        List<T> x = Arrays.asList(a);
        List<T> y = Arrays.asList(b);

        for (Diff.Range r : diff) {
            switch (r.operation) {
                case MATCH_AB -> result.addAll(x.subList(r.start, r.start + r.length));
                case DELETE_A -> { /* do nothing */ }
                case INSERT_B -> result.addAll(y.subList(r.start, r.start + r.length));
                default -> throw new AssertionError("Unknown operation " + r.operation);
            }
        }

        //noinspection unchecked
        return result.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), result.size()));
    }

    @NotNull
    private <T> T[] applyMatchAndRemoveForA(@NotNull  T[] a, @NotNull  T[] b, @NotNull  List<Diff.Range> diff) {
        List<T> result = new LinkedList<>();
        List<T> x = Arrays.asList(a);
        List<T> y = Arrays.asList(b);

        for (Diff.Range r : diff) {
            switch (r.operation) {
                case MATCH_AB, DELETE_A -> result.addAll(x.subList(r.start, r.start + r.length));
                case INSERT_B -> { /* do nothing */ }
                default -> throw new AssertionError("Unknown operation " + r.operation);
            }
        }

        //noinspection unchecked
        return result.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), result.size()));
    }


    @Test
    public void diffCases() {
        for (Pair<String, List<String>> testCase : testCases) {
            Character[] a = testCase.getFirst().chars().mapToObj(c -> (char) c).toArray(Character[]::new);
            for (String s : testCase.getSecond()) {
                Character[] b = s.chars().mapToObj(c -> (char) c).toArray(Character[]::new);

                List<Diff.Range> diff = Diff.prepareDiff(a, b, Character::equals);

                Assert.assertArrayEquals(b, this.applyForward(a, b, diff));
                Assert.assertArrayEquals(a, this.applyBackward(a, b, diff));
                Assert.assertArrayEquals(b, this.applyMatchAndInsertForB(a, b, diff));
                Assert.assertArrayEquals(a, this.applyMatchAndRemoveForA(a, b, diff));
            }
        }
    }
}
