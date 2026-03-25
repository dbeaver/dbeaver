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
package org.jkiss.dbeaver.model.virtual;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupRowStripingUtilsTest {

    @Test
    public void sameGroupKeyHandlesNullElements() {
        Assert.assertTrue(GroupRowStripingUtils.sameGroupKey(new Object[]{null}, new Object[]{null}));
        Assert.assertFalse(GroupRowStripingUtils.sameGroupKey(new Object[]{null}, new Object[]{"x"}));
    }

    @Test
    public void sameGroupKeySameReference() {
        Object[] k = new Object[]{1, 2};
        Assert.assertTrue(GroupRowStripingUtils.sameGroupKey(k, k));
    }

    @Test
    public void computeStripeIndicesEmptyReturnsEmpty() {
        Assert.assertArrayEquals(new int[0], GroupRowStripingUtils.computeStripeIndices(Collections.emptyList()));
    }

    @Test
    public void computeStripeIndicesConsecutiveRunsAlternates() {
        List<Object[]> keys = Arrays.asList(
            new Object[]{"A"},
            new Object[]{"A"},
            new Object[]{"B"},
            new Object[]{"B"}
        );
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1}, GroupRowStripingUtils.computeStripeIndices(keys));
    }

    @Test
    public void computeStripeIndicesInterleavedManyRuns() {
        List<Object[]> keys = Arrays.asList(
            new Object[]{"A"},
            new Object[]{"B"},
            new Object[]{"A"},
            new Object[]{"B"}
        );
        Assert.assertArrayEquals(new int[]{0, 1, 0, 1}, GroupRowStripingUtils.computeStripeIndices(keys));
    }

    @Test
    public void computeStripeIndicesCompositeKeyDuplicatePairsOneStripe() {
        List<Object[]> keys = Arrays.asList(
            new Object[]{"C1", 2},
            new Object[]{"C1", 2},
            new Object[]{"C1", 3}
        );
        Assert.assertArrayEquals(new int[]{0, 0, 1}, GroupRowStripingUtils.computeStripeIndices(keys));
    }

    @Test
    public void computeStripeIndicesAppendSegmentFirstRowContinuesParityFromPriorKey() {
        List<Object[]> first = Arrays.asList(
            new Object[]{"X"},
            new Object[]{"Y"}
        );
        int[] s1 = GroupRowStripingUtils.computeStripeIndices(first);
        Assert.assertArrayEquals(new int[]{0, 1}, s1);

        List<Object[]> second = Arrays.asList(
            new Object[]{"X"},
            new Object[]{"Y"},
            new Object[]{"Y"},
            new Object[]{"Z"}
        );
        int[] s2 = GroupRowStripingUtils.computeStripeIndices(second);
        Assert.assertArrayEquals(new int[]{0, 1, 1, 0}, s2);
    }

    @Test
    public void computeStripeIndicesThreeDistinctValues() {
        List<Object[]> keys = Arrays.asList(
            new Object[]{"A"},
            new Object[]{"B"},
            new Object[]{"C"}
        );
        Assert.assertArrayEquals(new int[]{0, 1, 0}, GroupRowStripingUtils.computeStripeIndices(keys));
    }

    @Test
    public void computeStripeIndicesRepeatedFirstValueAfterOthers() {
        List<Object[]> keys = Arrays.asList(
            new Object[]{"A"},
            new Object[]{"B"},
            new Object[]{"C"},
            new Object[]{"A"}
        );
        Assert.assertArrayEquals(new int[]{0, 1, 0, 1}, GroupRowStripingUtils.computeStripeIndices(keys));
    }
}
