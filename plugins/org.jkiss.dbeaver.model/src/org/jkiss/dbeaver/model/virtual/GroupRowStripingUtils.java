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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Alternating row backgrounds by consecutive runs of equal composite keys.
 */
public final class GroupRowStripingUtils {

    private GroupRowStripingUtils() {
    }

    /**
     * @param a composite key from one row (one entry per grouping column); may contain null elements
     * @param b same structure as {@code a}
     */
    public static boolean sameGroupKey(@Nullable Object[] a, @Nullable Object[] b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param keys ordered keys, one per visual row (same length as result set segment)
     * @return stripe index 0 or 1 per row; first run is 0, toggles when key changes
     */
    @NotNull
    public static int[] computeStripeIndices(@NotNull List<Object[]> keys) {
        int n = keys.size();
        int[] stripes = new int[n];
        if (n == 0) {
            return stripes;
        }
        int stripe = 0;
        Object[] prev = keys.get(0);
        stripes[0] = stripe;
        for (int i = 1; i < n; i++) {
            Object[] k = keys.get(i);
            if (!sameGroupKey(prev, k)) {
                stripe = 1 - stripe;
            }
            stripes[i] = stripe;
            prev = k;
        }
        return stripes;
    }
}
