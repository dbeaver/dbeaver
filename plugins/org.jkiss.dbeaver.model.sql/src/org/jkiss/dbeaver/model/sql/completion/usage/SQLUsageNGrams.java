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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trigram model of query tokens with interpolated backoff to bigrams and unigrams:
 * how likely a token follows the two tokens before it.
 */
final class SQLUsageNGrams {
    private static final String START = "^";
    // Interpolation weights of trigram, bigram and unigram estimates
    private static final double W3 = 0.6;
    private static final double W2 = 0.3;
    private static final double W1 = 0.1;

    // Context ("" for unigrams, "a" for bigrams, "a b" for trigrams) -> next token -> count
    final Map<String, Map<String, Integer>> counts = new HashMap<>();
    final Map<String, Integer> totals = new HashMap<>();

    void learn(@NotNull List<String> tokens) {
        String prev2 = START;
        String prev1 = START;
        for (String token : tokens) {
            add("", token);
            add(prev1, token);
            add(prev2 + " " + prev1, token);
            prev2 = prev1;
            prev1 = token;
        }
    }

    private void add(@NotNull String context, @NotNull String token) {
        counts.computeIfAbsent(context, k -> new HashMap<>()).merge(token, 1, Integer::sum);
        totals.merge(context, 1, Integer::sum);
    }

    /**
     * Probability of the token after the given tokens (only the last two are used).
     */
    double probability(@NotNull List<String> previous, @NotNull String token) {
        int size = previous.size();
        String prev1 = size > 0 ? previous.get(size - 1) : START;
        String prev2 = size > 1 ? previous.get(size - 2) : START;
        double p3 = estimate(prev2 + " " + prev1, token);
        double p2 = estimate(prev1, token);
        double p1 = estimate("", token);
        return W3 * p3 + W2 * p2 + W1 * p1;
    }

    private double estimate(@NotNull String context, @NotNull String token) {
        Integer total = totals.get(context);
        if (total == null || total == 0) {
            return 0;
        }
        Integer count = counts.get(context).get(token);
        return count == null ? 0 : (double) count / total;
    }
}
