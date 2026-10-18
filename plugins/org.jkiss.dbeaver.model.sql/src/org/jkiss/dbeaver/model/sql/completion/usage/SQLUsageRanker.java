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
package org.jkiss.dbeaver.model.sql.completion.usage;

import org.jkiss.code.NotNull;

/**
 * Linear ranking model learned online from pairs (chosen proposal, proposal it beat), with logistic loss
 * (RankNet with a linear scorer). It learns how much each usage feature counts for this user; weights stay
 * non-negative, so more usage never ranks a proposal lower. Bias and the position without usage are not
 * learned: proposals with the same score keep their order anyway.
 */
final class SQLUsageRanker {
    private static final double LEARNING_RATE = 0.05;
    private static final double L2 = 1e-4;

    final double[] weights;

    SQLUsageRanker(int featureCount) {
        weights = new double[featureCount];
        // Bias, frecency, co-occurrence, n-gram, position without usage
        double[] initial = {0, 1, 1, 3, 0};
        System.arraycopy(initial, 0, weights, 0, Math.min(initial.length, featureCount));
    }

    double score(@NotNull double[] x) {
        double s = 0;
        for (int i = 0; i < weights.length; i++) {
            s += weights[i] * x[i];
        }
        return s;
    }

    void learnPair(@NotNull double[] positive, @NotNull double[] negative) {
        double margin = score(positive) - score(negative);
        // Gradient of log(1 + exp(-margin))
        double g = 1.0 / (1.0 + Math.exp(margin));
        for (int i = 1; i < weights.length - 1; i++) {
            weights[i] = Math.max(0, weights[i] + LEARNING_RATE * (g * (positive[i] - negative[i]) - L2 * weights[i]));
        }
    }
}
