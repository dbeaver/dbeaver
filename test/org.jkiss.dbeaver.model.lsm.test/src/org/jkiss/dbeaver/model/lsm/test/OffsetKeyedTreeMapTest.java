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
package org.jkiss.dbeaver.model.lsm.test;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Test scenarios to cover OffsetKeyedTreeMap data structure
 *
 * orderings (M)
 *  - ascending
 *  - descending
 *  - random
 * <p>
 * modification scenarios (P)
 *  - all insertions (n)
 *  - all insertions, then all remove (different series proportions n,m)
 *  - intermixed insertions and remove (different series proportions n,m)
 *  - all insertions, then apply offset at existing key
 *  - all insertions, then apply offset at missing key
 *  - intermixed insertions, remove, apply offset (different series proportions n,m,p)
 *  - all insertions, then removeAtRange at existing key
 *  - all insertions, then removeAtRange offset at missing key
 *  - intermixed insertions, removeAtRange, apply offset (different series proportions n,m,p)
 * <p>
 * inspection scenarios (Q)
 *  - find existing key
 *  - find missing key
 *  - find existing key and iterate forward/backward till the end/head
 *  - find missing key and iterate forward/backward till the end/head
 *  - random backward-forward iterations
 * <p>
 *
 */
public class OffsetKeyedTreeMapTest extends DBeaverUnitTest {

    private static final Supplier<IntStream> SERIES = () -> Stream.of(
        IntStream.range(0, 26),
        IntStream.of(50, 100, 150)
    ).flatMapToInt(x -> x);

    private static final Supplier<IntStream> INTERMIXED_SERIES = () -> Stream.of(
        IntStream.of(50, 100, 150)
    ).flatMapToInt(x -> x);

    private static class Tester {
        public final TestScenarioBuilder sb = new TestScenarioBuilder();

        public void doInsertionsAllConsequentRemovals(Supplier<Stream<TestEntry>> insertions, boolean applyOffsets, boolean removalsForward) {
            int uniqueCount = (int) insertions.get().map(e -> e.offset).distinct().count();
            int count = (int) insertions.get().count();
            OffsetGeneratorParameters offsetParameters = OffsetGeneratorParameters.makeExclusiveAndExact(0, count, count / 2, 5);
            sb.appendInsertions(insertions);
            if (applyOffsets) {
                sb.appendApplyOffsetRandom(offsetParameters);
            }
            if (count < 10) {
                sb.appendCheckIteratorEverywhere(true);
                sb.appendCheckIteratorEverywhere(false);
            } else {
                sb.appendCheckIteratorAtRandomOffset(offsetParameters, true);
                sb.appendCheckIteratorAtRandomOffset(offsetParameters, false);
            }
            sb.appendRemovalsConsequent(0, uniqueCount, removalsForward);
            sb.complete().run();
        }

        public void doInsertionsAllRandomRemovals(Supplier<Stream<TestEntry>> insertions, boolean applyOffsets) {
            int uniqueCount = (int) insertions.get().map(e -> e.offset).distinct().count();
            int count = (int) insertions.get().count();
            sb.appendInsertions(insertions);
            sb.appendRemovalsRandom(0, uniqueCount, count < 10 ? count : uniqueCount / 2);
            sb.complete().run();
        }
    }

    @Test
    public void testInsertionsThenRemovals() {
        for (int i : SERIES.get().toArray()) {
            var t = new Tester();
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), false, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), false, false);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), false, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), false, false);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), false, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), false, false);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), false);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), false);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), false);
        }
    }

    @Test
    public void testInsertionsThenApplyOffsetsThenRemovals() {
        for (int i : SERIES.get().toArray()) {
            var t = new Tester();
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), true, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), true, false);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), true, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), true, false);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), true, true);
            t.doInsertionsAllConsequentRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), true, false);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorAscending(i, 1, 5), true);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorDescending(i, 1500, 5), true);
            t.doInsertionsAllRandomRemovals(t.sb.makeEntriesGeneratorRandom(i, 1, 500), true);
        }
    }

    @Test
    public void testIntermixedInsertionsAndRemovals() {
        for (int count : INTERMIXED_SERIES.get().toArray()) {
            TestScenarioBuilder sb = new TestScenarioBuilder();
            int presented = Math.max(25, count / 3);
            int dynamic = (count - presented);
            OffsetGeneratorParameters offsetParams = OffsetGeneratorParameters.makeExclusiveAndExact(5, presented - 5, dynamic, 3);

            sb.appendInsertions(sb.makeEntriesGeneratorRandom(presented, 1, 4000));
            sb.append(makeProportionalSeries(
                StreamSupplier.of(
                    sb.makeInsertions(sb.makeEntriesGeneratorRandom(dynamic, 1, 4000)),
                    sb.makeRemovalsRandom(0, presented, presented),
                    sb.makeCheckIteratorAtRandomOffset(offsetParams, true),
                    sb.makeCheckIteratorAtRandomOffset(offsetParams, false)
                ),
                // 6 iterations for 4 sets of operations, each turn for 1/6 of dynamically introduced entries
                () -> IntStream.range(0, 6 * 4).mapToDouble(n -> 100 / 6.0)
            ));
        }
    }

    @FunctionalInterface
    private interface ObjObjIntIntConsumer<A, B> {
        void accept(A a, B b, int n, int m);
    }

    private record TestOperation(int id, String description, boolean isMutation, Consumer<TestState> action) { }

    private static class Item {
        private static int idCounter = 0;
        private final int id;

        public Item() {
            this.id = ++idCounter;
        }

        @Override
        public String toString() {
            return "Item[" + this.id + "]";
        }
    }

    private interface IOKMCollection<T> {

        T find(int position);

        T put(int pos, T value);

        T put(int pos, T value, OffsetKeyedTreeMap.RemappingFunction<T> remappingFunction);

        OffsetKeyedTreeMap.NodesIterator<T> nodesIteratorAt(int position);

        int size();

        void applyOffset(int position, int delta);

        void forEach(BiConsumer<Integer, T> action);

        String collect();

        boolean removeAt(int position);

        void removeAtRange(int from, int to);

        List<Entry<T>> toListOfEntries();
    }

    private static class Entry<T> {
        public int offset;
        public T data;

        public Entry(int offset, T data) {
            this.offset = offset;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry<?> entry = (Entry<?>) o;
            return offset == entry.offset && Objects.equals(data, entry.data);
        }

        @Override
        public String toString() {
            return "Entry{" +
                "offset=" + offset +
                ", data=" + data +
                '}';
        }
    }

    private static class ArrayBackedOffsetKeyedCollection<T> implements IOKMCollection<T> {

        private final ArrayList<Entry<T>> list = new ArrayList<>();

        @Override
        public T find(int position) {
            int index = Collections.binarySearch(this.list, new Entry<>(position, null), Comparator.comparingInt(e -> e.offset));
            if (index < 0) {
                return null;
            } else {
                return this.list.get(index).data;
            }
        }

        @Override
        public T put(int pos, T value) {
            return this.put(pos, value, null);
        }

        @Override
        public T put(int pos, T value, OffsetKeyedTreeMap.RemappingFunction<T> remappingFunction) {
            int index = Collections.binarySearch(this.list, new Entry<>(pos, null), Comparator.comparingInt(e -> e.offset));
            if (index < 0) {
                index = ~index;
                list.add(index, new Entry<>(pos, value));
            } else {
                Entry<T> e = this.list.get(index);
                if (remappingFunction != null) {
                    value = remappingFunction.apply(e.offset, value, e.data);
                }
                e.data = value;
            }
            return value;
        }

        @Override
        public OffsetKeyedTreeMap.NodesIterator<T> nodesIteratorAt(int position) {
            return new OffsetKeyedTreeMap.NodesIterator<T>() {
                private boolean initial = true;
                private int index = STMUtils.binarySearchByKey(list, e -> e.offset, position, Comparator.comparingInt(x -> x));

                @Override
                public int getCurrOffset() {
                    if (this.initial) {
                        return position;
                    } else if (this.index >= 0 && this.index < list.size()) {
                        return list.get(this.index).offset;
                    } else if (this.index >= list.size()) {
                        return Integer.MAX_VALUE;
                    } else {
                        return Integer.MIN_VALUE;
                    }
                }

                @Nullable
                @Override
                public T getCurrValue() {
                    return this.index >= 0 && this.index < list.size() ? list.get(this.index).data : null;
                }

                @Override
                public boolean next() {
                    if (this.initial && this.index < 0) {
                        this.index = ~this.index;
                    } else {
                        this.index++;
                    }
                    this.initial = false;
                    return this.index < list.size();
                }

                @Override
                public boolean prev() {
                    if (this.initial && this.index < 0) {
                        this.index = ~this.index;
                    }
                    this.index--;
                    this.initial = false;
                    return this.index >= 0;
                }
            };
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public void applyOffset(int position, int delta) {
            int index = Collections.binarySearch(this.list, new Entry<>(position, null), Comparator.comparingInt(e -> e.offset));
            if (index < 0) {
                index = ~index;
            }
            if (delta < 0) {
                int end = Collections.binarySearch(this.list, new Entry<>(position - delta, null), Comparator.comparingInt(e -> e.offset));
                if (end < 0) {
                    end = ~end;
                }
                this.list.subList(index, end).clear();
            }
            this.list.subList(index, this.list.size()).forEach(e -> e.offset += delta);
        }

        @Override
        public void forEach(BiConsumer<Integer, T> action) {
            this.list.forEach(e -> action.accept(e.offset, e.data));
        }

        @Override
        public String collect() {
            return this.list.stream().map(e -> e.offset + ": " + e.data).collect(Collectors.joining(", ", "[", "]"));
        }

        @Override
        public boolean removeAt(int position) {
            return this.list.removeIf(e -> e.offset == position);
        }

        @Override
        public void removeAtRange(int from, int to) {
            this.list.removeIf(e -> e.offset >= from && e.offset < to);
        }

        @Override
        public List<Entry<T>> toListOfEntries() {
            return Collections.unmodifiableList(this.list);
        }
    }

    private static class TreeBackedOffsetKeyedCollection<T> implements IOKMCollection<T> {
        private final OffsetKeyedTreeMap<T> treeMap = new OffsetKeyedTreeMap<>();

        @Override
        public T find(int position) {
            return this.treeMap.find(position);
        }

        @Override
        public T put(int pos, T value) {
            return this.treeMap.put(pos, value);
        }

        @Override
        public T put(int pos, T value, OffsetKeyedTreeMap.RemappingFunction<T> remappingFunction) {
            return this.treeMap.put(pos, value, remappingFunction);
        }

        @Override
        public OffsetKeyedTreeMap.NodesIterator<T> nodesIteratorAt(int position) {
            return this.treeMap.nodesIteratorAt(position);
        }

        @Override
        public int size() {
            return this.treeMap.size();
        }

        @Override
        public void applyOffset(int position, int delta) {
            this.treeMap.applyOffset(position, delta);
        }

        @Override
        public void forEach(BiConsumer<Integer, T> action) {
            this.treeMap.forEach(action);
        }

        @Override
        public String collect() {
            return this.treeMap.collect();
        }

        @Override
        public boolean removeAt(int position) {
            return this.treeMap.removeAt(position);
        }

        @Override
        public void removeAtRange(int from, int to) {
            this.treeMap.applyOffset(from, to - from); /* see dropInvisibleScriptItems() */
        }

        @Override
        public List<Entry<T>> toListOfEntries() {
            List<Entry<T>> list = new ArrayList<>(this.treeMap.size());
            this.treeMap.forEach((x, y) -> {
                if (y != null) {
                    list.add(new Entry<>(x, y));
                }
            });
            return Collections.unmodifiableList(list);
        }
    }

    private static class TestEntry extends Entry<Item> {
        public TestEntry(int offset, Item data) {
            super(offset, data);
        }
    }

    private static <A, B, R> Stream<R> zip(Stream<A> a, Stream<B> b, BiFunction<A, B, R> function) {
        Iterator<A> x = a.iterator();
        Iterator<B> y = b.iterator();
        Iterator<R> it = new Iterator<R>() {
            @Override
            public boolean hasNext() {
                return x.hasNext() || y.hasNext();
            }

            @Override
            public R next() {
                return function.apply(x.hasNext() ? x.next() : null, y.hasNext() ? y.next() : null);
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }

    private static <T, R> Supplier<Stream<R>> mapIndexed(Supplier<Stream<T>> seq, BiFunction<Integer, T, R> function) {
        return () -> {
            Iterator<T> seqIt = seq.get().iterator();
            Iterator<R> it = new Iterator<R>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return seqIt.hasNext();
                }

                @Override
                public R next() {
                    return function.apply(index++, seqIt.next());
                }
            };
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
        };
    }

    private static <T> Supplier<Stream<T>> makeExactSeries(Supplier<Stream<Stream<T>>> sequences, Supplier<LongStream> seriesSizes) {
        return () -> {
            List<Iterator<T>> iterators = sequences.get().map(s -> s.iterator()).toList();

            class BatchIterator<T2> implements Iterator<T2> {
                private final long limit;
                private final Iterator<T2> it;
                private long index = 0;

                public BatchIterator(long limit, Iterator<T2> it) {
                    this.limit = limit;
                    this.it = it;
                }

                @Override
                public boolean hasNext() {
                    return this.index < this.limit && this.it.hasNext();
                }

                @Override
                public T2 next() {
                    this.index++;
                    return this.it.next();
                }
            }

            return mapIndexed(() -> seriesSizes.get().mapToObj(x -> x), (n, s) -> new BatchIterator<T>(s, iterators.get(n % iterators.size())))
                .get().flatMap(it -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false));
        };
    }

    private static <T> Supplier<Stream<T>> makeProportionalSeries(Supplier<Stream<Stream<T>>> sequences, Supplier<DoubleStream> proportionPercents) {
        Assert.assertTrue(sequences.get().allMatch(s -> s.spliterator().hasCharacteristics(Spliterator.SIZED)));
        return () -> {
            long[] streamSize = sequences.get().mapToLong(Stream::count).toArray();
            return makeExactSeries(
                sequences,
                () -> mapIndexed(
                    () -> proportionPercents.get().mapToObj(x -> x), (n, p) -> (long) (streamSize[n % streamSize.length] * p / 100.0)
                ).get().mapToLong(y -> y)
            ).get();
        };
    }

    private static <T> void assertCollectionsEqual(IOKMCollection<T> a, IOKMCollection<T> b) {
        assertEntryStreamsEqual(a.toListOfEntries().stream(), b.toListOfEntries().stream());
    }

    private static <T> void assertEntryStreamsEqual(Stream<Entry<T>> a, Stream<Entry<T>> b) {
        Assert.assertTrue(
            zip(
                a,
                b,
                (p, q) -> p != null && q != null && p.offset == q.offset && ((p.data == null && p.data == null) || (p.data != null && q.data != null && p.data.equals(q.data)))
            ).allMatch(c -> c));
    }

    private record OffsetGeneratorParameters(
            int indexFrom, boolean fromInclusive,
            int indexTo, boolean toInclusive,
            int amount, int deltaLimit, boolean excludeExact) {

        public static OffsetGeneratorParameters makeInclusiveAndExact(int indexFrom, int indexTo, int amount, int deltaLimit) {
            return new OffsetGeneratorParameters(indexFrom, true, indexTo, true, amount, deltaLimit, false);
        }

        public static OffsetGeneratorParameters makeInclusiveNoExact(int indexFrom, int indexTo, int amount, int deltaLimit) {
            return new OffsetGeneratorParameters(indexFrom, true, indexTo, true, amount, deltaLimit, true);
        }

        public static OffsetGeneratorParameters makeExclusiveAndExact(int indexFrom, int indexTo, int amount, int deltaLimit) {
            return new OffsetGeneratorParameters(indexFrom, false, indexTo, false, amount, deltaLimit, false);
        }

        public static OffsetGeneratorParameters makeExclusiveNoExact(int indexFrom, int indexTo, int amount, int deltaLimit) {
            return new OffsetGeneratorParameters(indexFrom, false, indexTo, false, amount, deltaLimit, true);
        }

        public static OffsetGeneratorParameters makeBeforeFirst() {
            return new OffsetGeneratorParameters(-1, true, -1, false, 1, 50, true);
        }

        public static OffsetGeneratorParameters makeAfterLast() {
            return new OffsetGeneratorParameters(Integer.MAX_VALUE, true, Integer.MAX_VALUE, false, 1, 50, true);
        }
    }

    private static class TestScenarioBuilder {
        private static final Random staticRandom = new Random(12345);

        private Stream<TestOperation> ops = Stream.empty();

        private int opsCounter = 0;

        private TestOperation makeOperation(String description, boolean isMutation, Consumer<TestState> operation) {
            return new TestOperation(++this.opsCounter, description, isMutation, operation);
        }

        public void append(TestOperation op) {
            this.ops = Stream.concat(this.ops, Stream.of(op));
        }

        public void append(Supplier<Stream<TestOperation>> ops) {
            this.ops = Stream.concat(this.ops, ops.get());
        }

        public void appendInsertions(Supplier<Stream<TestEntry>> entries) {
            this.append(this.makeInsertions(entries));
        }

        public Supplier<Stream<TestOperation>> makeInsertions(Supplier<Stream<TestEntry>> entries) {
            return () -> entries.get().map(p -> this.makeOperation("insert " + p.data + " at " + p.offset, true, s -> {
                s.arrayBacked.put(p.offset, p.data);
                s.treeBacked.put(p.offset, p.data);
            }));
        }

        public void appendRemovalsConsequent(int index, int amount, boolean forward) {
            this.append(this.makeRemovalsConsequent(index, amount, forward));
        }

        public Supplier<Stream<TestOperation>> makeRemovalsConsequent(int index, int amount, boolean forward) {
            return () -> IntStream.range(0, amount).mapToObj(n -> this.makeOperation("remove at offset of entry #" + (forward ? index : (index - n)), true, s -> {
                var entry = s.arrayBacked.toListOfEntries().get(forward ? index : Math.max(0, index - n));
                s.arrayBacked.removeAt(entry.offset);
                s.treeBacked.removeAt(entry.offset);
            }));
        }

        public void appendRemovalsRandom(int indexFrom, int indexTo, int amount) {
            this.append(this.makeRemovalsRandom(indexFrom, indexTo, amount));
        }

        public Supplier<Stream<TestOperation>> makeRemovalsRandom(int indexFrom, int indexTo, int amount) {
            if (indexTo - indexFrom < amount) {
                throw new IllegalArgumentException();
            }
            int seed = staticRandom.nextInt();
            return () -> {
                var rnd = new Random(seed);
                var indexes = Interval.of(indexFrom, indexTo);
                return IntStream.range(0, amount).mapToObj(n -> this.makeOperation("remove at offset of random entry #" + indexFrom + "-#" + (indexTo - n), true, s -> {
                    var entry = s.arrayBacked.toListOfEntries().get(rnd.nextInt(indexes.a, indexes.b));
                    s.arrayBacked.removeAt(entry.offset);
                    s.treeBacked.removeAt(entry.offset);
                    indexes.b--;
                }));
            };
        }

        public void appendApplyOffsetAt(int pos, int delta) {
            this.append(this.makeOperation("apply offset at " + pos + " with delta " + delta, true, s -> {
                s.arrayBacked.applyOffset(pos, delta);
                s.treeBacked.applyOffset(pos, delta);
            }));
        }

        public void appendApplyOffsetRandom(OffsetGeneratorParameters offsetParams) {
            this.append(this.makeApplyOffsetRandom(offsetParams));
        }

        public Supplier<Stream<TestOperation>> makeApplyOffsetRandom(OffsetGeneratorParameters offsetParams) {
            int seed = staticRandom.nextInt();
            return () -> {
                Random rnd = new Random(seed);
                return this.makeOpsAtRandomOffsets(
                    offsetParams, "apply offset at random entry #" + offsetParams.indexFrom + "-#" + offsetParams.indexTo, true, (s, entry, offset, index) -> {
                        int delta = rnd.nextInt(1, offsetParams.deltaLimit);

                        s.arrayBacked.applyOffset(offset, delta);
                        s.treeBacked.applyOffset(offset, delta);
                    }
                ).get();
            };
        }

        public Supplier<Stream<TestOperation>> makeOpsAtRandomOffsets(
            OffsetGeneratorParameters offsetParams,
            String description,
            boolean isMutation,
            ObjObjIntIntConsumer<TestState, Entry<Item>> action
        ) {

            return () -> {
                var rnd = new Random(staticRandom.nextInt());
                var indexes = Interval.of(
                    offsetParams.fromInclusive ? offsetParams.indexFrom - 1 : offsetParams.indexFrom,
                    offsetParams.toInclusive ? offsetParams.indexTo : offsetParams.indexTo + 1
                );
                return IntStream.range(0, offsetParams.amount).mapToObj(n -> this.makeOperation(description, isMutation, s -> {
                    int entryIndex = rnd.nextInt(indexes.a, indexes.b);
                    var entries = s.arrayBacked.toListOfEntries();
                    // TODO consider limits
                    var entry = entryIndex >= 0 && entryIndex < entries.size() ? entries.get(entryIndex) : null;
                    var nextEntry = entryIndex + 1 >= 0 && entryIndex + 1 < entries.size() ? entries.get(entryIndex + 1) : null;

                    int offsetFrom, offsetTo;
                    if (entry == null) {
                        if (nextEntry == null) {
                            offsetFrom = entries.get(entries.size() - 1).offset;
                            offsetTo = Integer.MAX_VALUE / 2;
                        } else {
                            offsetFrom = 0;
                            offsetTo = nextEntry.offset;
                        }
                    } else {
                        offsetFrom = offsetParams.excludeExact ? entry.offset + 1 : entry.offset;
                        if (nextEntry == null) {
                            offsetTo = entry.offset + offsetParams.deltaLimit;
                        } else {
                            offsetTo = nextEntry.offset;
                        }
                    }

                    int offset = rnd.nextInt(offsetFrom, offsetTo);
                    action.accept(s, entry != null && entry.offset == offset ? entry : null, offset, entryIndex);
                }));
            };
        }

        public TestScenario complete() {
            TestScenario result = new TestScenario(this.ops.toList());
            this.ops = Stream.empty();
            return result;
        }

        public void appendAssertionIteratorNotNullAt(int pos) {
            this.append(this.makeOperation("assert interator not null at " + pos, false, s -> {
                var it = s.treeBacked.nodesIteratorAt(pos);
                Assert.assertNotNull(it.getCurrValue());
            }));
        }

        public void appendCheckFind(int pos) {
            this.append(this.makeOperation("check find at " + pos, false, s -> {
                Assert.assertEquals(s.arrayBacked.find(pos), s.treeBacked.find(pos));
            }));
        }

        public void appendCheckFindRandomOffset(OffsetGeneratorParameters offsetParams) {
            this.append(this.makeCheckFindRandomOffset(offsetParams));
        }

        public Supplier<Stream<TestOperation>> makeCheckFindRandomOffset(OffsetGeneratorParameters offsetParams) {
            return this.makeOpsAtRandomOffsets(
                offsetParams, "check find offset at random entry #" + offsetParams.indexFrom + "-#" + offsetParams.indexTo, true,  (s, entry, offset, index) -> {
                    Assert.assertEquals(entry == null ? null : entry.data, s.treeBacked.find(offset));
                }
            );
        }

        public void appendCheckIteratorEverywhere(boolean forward) {
            this.append(this.makeOperation("test iterator everywhere", false, s -> {
                List<Entry<Item>> entries = s.arrayBacked.toListOfEntries();
                for (int i = 0; i < entries.size(); i++) {
                    Entry<Item> entry = entries.get(i);
                    Entry<Item> prevEntry = i <= 0 || entries.get(i - 1).offset != entry.offset - 1 ? null : entries.get(i - 1);
                    Entry<Item> nextEntry = i >= entries.size() - 1 || entries.get(i + 1).offset != entry.offset + 1 ? null : entries.get(i + 1);
                    this.checkIteratorAtOffset(forward, s, prevEntry, entry.offset - 1, i);
                    this.checkIteratorAtOffset(forward, s, entry, entry.offset, i);
                    this.checkIteratorAtOffset(forward, s, nextEntry, entry.offset + 1, i);
                }
            }));
        }

        public void appendCheckIteratorAtRandomOffset(OffsetGeneratorParameters offsetParams, boolean forward) {
            this.append(this.makeCheckIteratorAtRandomOffset(offsetParams, forward));
        }

        public Supplier<Stream<TestOperation>> makeCheckIteratorAtRandomOffset(OffsetGeneratorParameters offsetParams, boolean forward) {
            return this.makeOpsAtRandomOffsets(
                offsetParams, "check "  + (forward ? "forward" : "backward") + " iterator from offset at random entry #" + offsetParams.indexFrom + "-#" + offsetParams.indexTo, false,  (s, entry, offset, index) -> {
                    this.checkIteratorAtOffset(forward, s, entry, offset, index);
                }
            );
        }

        private void checkIteratorAtOffset(boolean forward, TestState s, Entry<Item> entry, int offset, int index) {
            Predicate<OffsetKeyedTreeMap.NodesIterator<Item>> nextOp = forward ? it -> it.next() : it -> it.prev();
            OffsetKeyedTreeMap.NodesIterator<Item> it1 = s.treeBacked.nodesIteratorAt(offset);
            OffsetKeyedTreeMap.NodesIterator<Item> it2 = s.arrayBacked.nodesIteratorAt(offset);
            Item presentedValueAtOffset = it1.getCurrValue();
            Item expectedValueAtOffset = it2.getCurrValue();
            Assert.assertEquals(entry == null ? null : entry.data, presentedValueAtOffset);
            Assert.assertEquals(expectedValueAtOffset, presentedValueAtOffset);
            if (it1.getCurrValue() != null) {
                Assert.assertEquals(offset, it1.getCurrOffset());
            }
            LinkedList<Entry<Item>> presentedEntries = new LinkedList<>();
            LinkedList<Entry<Item>> expectedEntries = new LinkedList<>();
            boolean a, b;
            do {
                a = nextOp.test(it1);
                b = nextOp.test(it2);
                Assert.assertEquals(a, b);
                presentedEntries.addLast(new Entry<>(it1.getCurrOffset(), it1.getCurrValue()));
                expectedEntries.addLast(new Entry<>(it2.getCurrOffset(), it2.getCurrValue()));
            } while (a && b);
            assertEntryStreamsEqual(expectedEntries.stream(), presentedEntries.stream());
        }

        public Supplier<Stream<TestEntry>> makeEntriesGeneratorAscending(int count, int from, int step) {
            return () -> IntStream.range(0, count).mapToObj(n -> new TestEntry(from + n * step, new Item()));
        }

        public Supplier<Stream<TestEntry>> makeEntriesGeneratorDescending(int count, int from, int step) {
            return () -> IntStream.range(0, count).mapToObj(n -> new TestEntry(from - n * step, new Item()));
        }

        public Supplier<Stream<TestEntry>> makeEntriesGeneratorRandom(int count, int min, int max) {
            if (max - min < count) {
                throw new IllegalArgumentException();
            }

            int seed = staticRandom.nextInt();
            return () -> {
                Random rnd = new Random(seed);
                return IntStream.range(1, count).mapToObj(n -> new TestEntry(rnd.nextInt(min, max), new Item()));
            };
        }
    }

    private static class TestState {
        public final IOKMCollection<Item> arrayBacked = new ArrayBackedOffsetKeyedCollection<Item>();
        public final IOKMCollection<Item> treeBacked = new TreeBackedOffsetKeyedCollection<Item>();
    }

    private static class TestScenario {
        private final List<TestOperation> ops;

        public TestScenario(List<TestOperation> ops) {
            this.ops = ops;
        }

        public void run() { // boolean selectionsAfterEachStep) {
            // add run log if needed
            TestState state = new TestState();

            boolean isLastOpMutation = true;
            for (int i = 0; i < this.ops.size(); i++) {
                TestOperation op = this.ops.get(i);
                op.action.accept(state);
                isLastOpMutation = op.isMutation;
                if (isLastOpMutation) {
                    this.checkCollectionState(state);
                }
            }
            if (!isLastOpMutation) {
                this.checkCollectionState(state);
            }
        }

        private void checkCollectionState(TestState state) {
            assertCollectionsEqual(state.arrayBacked, state.treeBacked);
        }

    }

    private static class StreamSupplier {
        public static <T> Supplier<Stream<Stream<T>>> of(Supplier<Stream<T>> ... sources) {
            return () -> Stream.of(sources).map(Supplier::get);
        }
    }
}

