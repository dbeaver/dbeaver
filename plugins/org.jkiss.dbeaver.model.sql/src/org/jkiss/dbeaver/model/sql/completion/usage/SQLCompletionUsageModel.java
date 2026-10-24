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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the user of one connection uses in SQL: objects, joins and token sequences, learned from executed queries
 * and from accepted completion proposals. Ranks completion proposals by it. Everything is local and small:
 * counters with exponential decay, a trigram model and a linear ranker, no external service.
 *
 * <p>Objects are identified by normalized keys (see {@link SQLUsageQuery}): {@code t:schema.table},
 * {@code c:schema.table.column}. Keywords are kept as upper case words.</p>
 *
 * <p>Not thread safe: callers synchronize.</p>
 */
public class SQLCompletionUsageModel {

    /**
     * Parts of the model used for ranking. All of them learn; disabled ones do not contribute to scores
     * (used to measure each part).
     */
    public enum Feature {
        FRECENCY,
        COOCCURRENCE,
        NGRAM,
        RANKER
    }

    static final int FEATURE_COUNT = 5;
    // N-gram probabilities (0..1) are small next to log counts
    static final double NGRAM_WEIGHT = 3;

    // Uses older than a few half-lives weigh almost nothing
    private final double halfLifeMs;
    private final boolean[] enabled = new boolean[Feature.values().length];

    // Decayed use counts of objects
    final Map<String, Frecency> objects = new HashMap<>();
    // Decayed counts of objects used together with a table in one query (joined tables)
    final Map<String, Map<String, Frecency>> cooccurrence = new HashMap<>();
    // Container path and name of used tables (original case)
    final Map<String, List<String>> tablePaths = new HashMap<>();
    final SQLUsageNGrams ngrams = new SQLUsageNGrams();
    final SQLUsageRanker ranker = new SQLUsageRanker(FEATURE_COUNT);

    /**
     * Decayed counter: value at {@code time} is {@code score * 2^(-(now - time) / halfLife)}.
     */
    static final class Frecency {
        double score;
        long time;

        double valueAt(long now, double halfLifeMs) {
            return now <= time ? score : score * Math.pow(2, -(now - time) / halfLifeMs);
        }

        void add(double weight, long now, double halfLifeMs) {
            score = valueAt(now, halfLifeMs) + weight;
            time = Math.max(time, now);
        }
    }

    public SQLCompletionUsageModel(long halfLifeMs) {
        this.halfLifeMs = halfLifeMs;
        for (Feature feature : Feature.values()) {
            enabled[feature.ordinal()] = true;
        }
    }

    public void setEnabled(@NotNull Feature feature, boolean enable) {
        enabled[feature.ordinal()] = enable;
    }

    public boolean isEnabled(@NotNull Feature feature) {
        return enabled[feature.ordinal()];
    }

    /**
     * Learns from an executed query.
     */
    public void learnQuery(@NotNull SQLUsageQuery query, long time) {
        for (String object : query.getObjects()) {
            objects.computeIfAbsent(object, k -> new Frecency()).add(1, time, halfLifeMs);
        }
        List<String> tables = query.getTables();
        for (String table : tables) {
            for (String other : tables) {
                if (!other.equals(table)) {
                    cooccurrence.computeIfAbsent(table, k -> new HashMap<>())
                        .computeIfAbsent(other, k -> new Frecency()).add(1, time, halfLifeMs);
                }
            }
        }
        ngrams.learn(query.getTokens());
        tablePaths.putAll(query.getTablePaths());
    }

    /**
     * Learns that the user used objects (e.g. chose them in the completion popup): they rank higher at once,
     * before the query is complete.
     */
    public void learnUse(@NotNull Collection<String> keys, long time) {
        for (String key : keys) {
            objects.computeIfAbsent(key, k -> new Frecency()).add(1, time, halfLifeMs);
        }
    }

    /**
     * Paths (container names and table name) of the tables used most, most used first.
     */
    @NotNull
    public List<List<String>> getTopTables(int limit, long time) {
        List<Map.Entry<String, List<String>>> tables = new ArrayList<>(tablePaths.entrySet());
        tables.removeIf(e -> !objects.containsKey(e.getKey()));
        tables.sort((e1, e2) -> Double.compare(
            objects.get(e2.getKey()).valueAt(time, halfLifeMs), objects.get(e1.getKey()).valueAt(time, halfLifeMs)));
        List<List<String>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : tables.subList(0, Math.min(limit, tables.size()))) {
            result.add(e.getValue());
        }
        return result;
    }

    /**
     * Learns from a completion popup: the user chose {@code chosen} among {@code shown} (in the order shown).
     * Only the ranker learns from it: the query with the chosen object is learned when it is executed.
     */
    public void learnChoice(
        @NotNull SQLUsageContext context,
        @NotNull List<String> shown,
        @NotNull String chosen,
        long time
    ) {
        int chosenIndex = shown.indexOf(chosen);
        if (chosenIndex < 0) {
            return;
        }
        double[] positive = features(context, chosen, chosenIndex, shown.size(), time);
        // Proposals shown above the chosen one, and a few below it, are the ones it beat
        int last = Math.min(shown.size(), chosenIndex + 4);
        for (int i = 0; i < last; i++) {
            if (i != chosenIndex) {
                ranker.learnPair(positive, features(context, shown.get(i), i, shown.size(), time));
            }
        }
    }

    /**
     * Usage score of a proposal: 0 for objects never used in the context, higher is better.
     *
     * @param baseRank position in the list ordered without usage (0 is first)
     */
    public double score(@NotNull SQLUsageContext context, @NotNull String candidate, int baseRank, int listSize, long time) {
        double[] x = features(context, candidate, baseRank, listSize, time);
        if (x[1] == 0 && x[2] == 0 && x[3] == 0) {
            // Never used here: keep the order of the list
            return 0;
        }
        if (isEnabled(Feature.RANKER)) {
            return ranker.score(x);
        }
        return x[1] * (isEnabled(Feature.FRECENCY) ? 1 : 0) +
            x[2] * (isEnabled(Feature.COOCCURRENCE) ? 1 : 0) +
            x[3] * (isEnabled(Feature.NGRAM) ? NGRAM_WEIGHT : 0);
    }

    /**
     * Orders proposals by usage score. Proposals with the same score (e.g. never used) keep their order.
     */
    @NotNull
    public List<String> rank(@NotNull SQLUsageContext context, @NotNull List<String> proposals, long time) {
        double[] scores = new double[proposals.size()];
        List<Integer> order = new ArrayList<>(proposals.size());
        for (int i = 0; i < proposals.size(); i++) {
            scores[i] = score(context, proposals.get(i), i, proposals.size(), time);
            order.add(i);
        }
        order.sort((i1, i2) -> {
            int rc = Double.compare(scores[i2], scores[i1]);
            return rc != 0 ? rc : Integer.compare(i1, i2);
        });
        List<String> result = new ArrayList<>(proposals.size());
        for (int i : order) {
            result.add(proposals.get(i));
        }
        return result;
    }

    /**
     * Features of a proposal: bias, frecency, co-occurrence with the tables of the query, n-gram probability,
     * position in the list without usage.
     */
    @NotNull
    double[] features(@NotNull SQLUsageContext context, @NotNull String candidate, int baseRank, int listSize, long time) {
        double[] x = new double[FEATURE_COUNT];
        x[0] = 1;
        if (isEnabled(Feature.FRECENCY) || isEnabled(Feature.RANKER)) {
            Frecency frecency = objects.get(candidate);
            x[1] = frecency == null ? 0 : Math.log1p(frecency.valueAt(time, halfLifeMs));
        }
        if (isEnabled(Feature.COOCCURRENCE) || isEnabled(Feature.RANKER)) {
            x[2] = Math.log1p(cooccurrence(context.getTables(), candidate, time));
        }
        if (isEnabled(Feature.NGRAM) || isEnabled(Feature.RANKER)) {
            x[3] = ngrams.probability(context.getPreviousTokens(), candidate);
        }
        x[4] = listSize <= 1 ? 0 : 1.0 - (double) baseRank / (listSize - 1);
        return x;
    }

    private double cooccurrence(@NotNull Collection<String> tables, @NotNull String candidate, long time) {
        String candidateTable = SQLUsageQuery.tableOf(candidate);
        double result = 0;
        for (String table : tables) {
            Map<String, Frecency> related = cooccurrence.get(table);
            if (related != null && candidateTable != null && !candidateTable.equals(table)) {
                Frecency frecency = related.get(candidateTable);
                if (frecency != null) {
                    result += frecency.valueAt(time, halfLifeMs);
                }
            }
        }
        return result;
    }

    @Nullable
    Frecency getObject(@NotNull String key) {
        return objects.get(key);
    }

    public double getHalfLifeMs() {
        return halfLifeMs;
    }

    /**
     * Writes the model as JSON, pruned to its most used entries.
     */
    public void write(@NotNull Writer writer, long now) throws IOException {
        try (JsonWriter json = new JsonWriter(writer)) {
            SQLUsageModelSerializer.write(this, json, now);
        }
    }

    /**
     * Reads a model written by {@link #write}.
     */
    public void read(@NotNull Reader reader) throws IOException {
        try (JsonReader json = new JsonReader(reader)) {
            SQLUsageModelSerializer.read(this, json);
        }
    }
}
