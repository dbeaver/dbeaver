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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://neil.fraser.name/writing/diff/myers.pdf">See Eugene Myers' "An O(ND) Difference Algorithm and Its Variations"</a>
 *
 * @param <T> the type of the element of the sequences to compare
 */
public class Diff<T> {

    /**
     * Comparator returning boolean as a result of comparison
     *
     * @param <T> the type of the element of the sequences to compare
     */
    public interface EqualityComparator<T> {
        boolean equals(@NotNull T a, @NotNull T b);
    }

    public enum Operation {
        MATCH_AB,
        DELETE_A,
        INSERT_B
    }

    public static class Range {
        public Operation operation;
        public int start; /* index in A when MATCH or DELETE, index in B when INSERT */
        public int length;
    }

    private static class MiddleFragment {
        public int x;
        public int y;
        public int u;
        public int v;
    }

    private final T[] a;
    private final T[] b;

    private final EqualityComparator<T> comparator;
    private final List<Integer> reachedEndPoints = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();

    private final int dmax;

    private Diff(@NotNull T[] a, @NotNull T[] b, @NotNull EqualityComparator<T> comparator, int dmax) {
        this.a = a;
        this.b = b;
        this.comparator = comparator;
        this.dmax = dmax;
    }

    private void setReached(int k, int r, int x) {
        // Pack -N to N into 0 to N * 2
        int j = k <= 0 ? -k * 4 + r : k * 4 + (r - 2);

        while (this.reachedEndPoints.size() <= j) {
            this.reachedEndPoints.add(0);
        }

        this.reachedEndPoints.set(j, x);
    }

    private int getReached(int k, int r) {
        int j = k <= 0 ? -k * 4 + r : k * 4 + (r - 2);
        return this.reachedEndPoints.get(j);
    }

    private int FV(int k) {
        return this.getReached(k, 0);
    }

    private int RV(int k) {
        return this.getReached(k, 1);
    }

    private void submitRange(@NotNull Operation op, int off, int len) {
        if (len == 0) {
            return;
        }
        // Add new range entry or merge with existing if the op is the same

        Range e = this.ranges.isEmpty() ? null : this.ranges.getLast();
        if (e != null && e.operation == op) {
            e.length += len;
        } else {
            e = new Range();
            e.operation = op;
            e.start = off;
            e.length = len;
            this.ranges.add(e);
        }
    }

    private int findMiddleFragment(int aOff, int aLen, int bOff, int bLen, @NotNull MiddleFragment ms) {
        int delta;
        int odd;
        int mid;

        delta = aLen - bLen;
        odd = delta & 1;
        mid = (aLen + bLen) / 2;
        mid += odd;

        this.setReached(1, 0, 0);
        this.setReached(delta - 1, 1, aLen);

        for (int d = 0; d <= mid; d++) {
            int k;
            int x;
            int y;

            if ((2 * d - 1) >= this.dmax) {
                return this.dmax;
            }

            for (k = d; k >= -d; k -= 2) {
                if (k == -d || (k != d && FV(k - 1) < FV(k + 1))) {
                    x = FV(k + 1);
                } else {
                    x = FV(k - 1) + 1;
                }
                y = x - k;

                ms.x = x;
                ms.y = y;
                while (x < aLen && y < bLen && this.comparator.equals(this.a[aOff + x], this.b[bOff + y])) {
                    x++;
                    y++;
                }
                this.setReached(k, 0, x);

                if (odd != 0 && k >= (delta - (d - 1)) && k <= (delta + (d - 1))) {
                    if (x >= RV(k)) {
                        ms.u = x;
                        ms.v = y;
                        return 2 * d - 1;
                    }
                }
            }
            for (k = d; k >= -d; k -= 2) {
                int kr = (aLen - bLen) + k;

                if (k == d || (k != -d && RV(kr - 1) < RV(kr + 1))) {
                    x = RV(kr - 1);
                } else {
                    x = RV(kr + 1) - 1;
                }
                y = x - kr;

                ms.u = x;
                ms.v = y;
                while (x > 0 && y > 0 && this.comparator.equals(this.a[aOff + (x - 1)], this.b[bOff + (y - 1)])) {
                    x--;
                    y--;
                }
                this.setReached(kr, 1, x);

                if (odd == 0 && kr >= -d && kr <= d) {
                    if (x <= FV(kr)) {
                        ms.x = x;
                        ms.y = y;
                        return 2 * d;
                    }
                }
            }
        }

        return -1;
    }

    private int processFragment(int aOff, int aLen, int bOff, int bLen) {
        MiddleFragment ms = new MiddleFragment();
        int d;

        if (aLen == 0) {
            this.submitRange(Operation.INSERT_B, bOff, bLen);
            d = bLen;
        } else if (bLen == 0) {
            this.submitRange(Operation.DELETE_A, aOff, aLen);
            d = aLen;
        } else {
            // Find the middle "snake" around which we recursively solve the sub-problems.
            d = this.findMiddleFragment(aOff, aLen, bOff, bLen, ms);
            if (d == -1) {
                return -1;
            } else if (d >= this.dmax) {
                return this.dmax;
            } else if (d > 1) {
                if (this.processFragment(aOff, ms.x, bOff, ms.y) == -1) {
                    return -1;
                }

                this.submitRange(Operation.MATCH_AB, aOff + ms.x, ms.u - ms.x);

                aOff += ms.u;
                bOff += ms.v;
                aLen -= ms.u;
                bLen -= ms.v;
                if (this.processFragment(aOff, aLen, bOff, bLen) == -1) {
                    return -1;
                }
            } else {
                int x = ms.x;
                int u = ms.u;

                if (bLen > aLen) {
                    if (x == u) {
                        this.submitRange(Operation.MATCH_AB, aOff, aLen);
                        this.submitRange(Operation.INSERT_B, bOff + (bLen - 1), 1);
                    } else {
                        this.submitRange(Operation.INSERT_B, bOff, 1);
                        this.submitRange(Operation.MATCH_AB, aOff, aLen);
                    }
                } else {
                    if (x == u) {
                        this.submitRange(Operation.MATCH_AB, aOff, bLen);
                        this.submitRange(Operation.DELETE_A, aOff + (aLen - 1), 1);
                    } else {
                        this.submitRange(Operation.DELETE_A, aOff, 1);
                        this.submitRange(Operation.MATCH_AB, aOff + 1, bLen);
                    }
                }
            }
        }

        return d;
    }

    @NotNull
    public static <T> List<Range> prepareDiff(@NotNull T[] a, @NotNull T[] b, @NotNull EqualityComparator<T> comparator) {
        return prepareDiff(a, 0, a.length, b, 0, b.length, comparator);
    }

    @NotNull
    public static <T> List<Range> prepareDiff(
        @NotNull T[] a,
        int aOffset,
        int aLength,
        @NotNull T[] b,
        int bOffset,
        int bLength,
        @NotNull EqualityComparator<T> comparator
    ) {
        var d = new Diff<>(a, b, comparator, Integer.MAX_VALUE);
        d.prepareDiffImpl(aOffset, aLength, bOffset, bLength);
        return d.ranges;
    }

    private void prepareDiffImpl(int aOff, int aLen, int bOff, int bLen) {
        int x = 0;
        int y = 0;
        while (x < aLen && y < bLen && this.comparator.equals(a[aOff + x], b[bOff + y])) {
            x++;
            y++;
        }
        this.submitRange(Operation.MATCH_AB, aOff, x);
        this.processFragment(aOff + x, aLen - x, bOff + y, bLen - y);
    }
}
