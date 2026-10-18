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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads {@link SQLCompletionUsageModel} as JSON. Before writing, the model is pruned to its most used
 * entries, so the file stays small however long it is used.
 */
final class SQLUsageModelSerializer {
    static final int FORMAT_VERSION = 1;
    static final int MAX_OBJECTS = 20_000;
    static final int MAX_RELATED = 50;
    static final int MAX_NGRAM_CONTEXTS = 50_000;
    static final int MAX_NGRAM_FOLLOWERS = 50;

    private SQLUsageModelSerializer() {
    }

    static void write(@NotNull SQLCompletionUsageModel model, @NotNull JsonWriter writer, long now) throws IOException {
        writer.beginObject();
        writer.name("version").value(FORMAT_VERSION);
        writer.name("objects");
        writeFrecencies(writer, top(model.objects, MAX_OBJECTS, model, now));
        writer.name("related").beginObject();
        for (Map.Entry<String, Map<String, SQLCompletionUsageModel.Frecency>> e : model.cooccurrence.entrySet()) {
            if (model.objects.containsKey(e.getKey())) {
                writer.name(e.getKey());
                writeFrecencies(writer, top(e.getValue(), MAX_RELATED, model, now));
            }
        }
        writer.endObject();
        writer.name("ngrams").beginObject();
        List<Map.Entry<String, Integer>> contexts = new ArrayList<>(model.ngrams.totals.entrySet());
        contexts.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        for (Map.Entry<String, Integer> context : contexts.subList(0, Math.min(contexts.size(), MAX_NGRAM_CONTEXTS))) {
            List<Map.Entry<String, Integer>> followers = new ArrayList<>(model.ngrams.counts.get(context.getKey()).entrySet());
            followers.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
            writer.name(context.getKey()).beginObject();
            for (Map.Entry<String, Integer> f : followers.subList(0, Math.min(followers.size(), MAX_NGRAM_FOLLOWERS))) {
                writer.name(f.getKey()).value(f.getValue());
            }
            writer.endObject();
        }
        writer.endObject();
        writer.name("paths").beginObject();
        for (Map.Entry<String, List<String>> e : model.tablePaths.entrySet()) {
            if (model.objects.containsKey(e.getKey())) {
                writer.name(e.getKey()).beginArray();
                for (String name : e.getValue()) {
                    writer.value(name);
                }
                writer.endArray();
            }
        }
        writer.endObject();
        writer.name("weights").beginArray();
        for (double w : model.ranker.weights) {
            writer.value(w);
        }
        writer.endArray();
        writer.endObject();
    }

    static void read(@NotNull SQLCompletionUsageModel model, @NotNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "version" -> {
                    if (reader.nextInt() != FORMAT_VERSION) {
                        throw new IOException("Unsupported completion usage format");
                    }
                }
                case "objects" -> readFrecencies(reader, model.objects);
                case "related" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String table = reader.nextName();
                        readFrecencies(reader, model.cooccurrence.computeIfAbsent(table, k -> new HashMap<>()));
                    }
                    reader.endObject();
                }
                case "ngrams" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        final String context = reader.nextName();
                        Map<String, Integer> followers = new HashMap<>();
                        int total = 0;
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String token = reader.nextName();
                            int count = reader.nextInt();
                            followers.put(token, count);
                            total += count;
                        }
                        reader.endObject();
                        model.ngrams.counts.put(context, followers);
                        model.ngrams.totals.put(context, total);
                    }
                    reader.endObject();
                }
                case "paths" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        final String key = reader.nextName();
                        List<String> path = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            path.add(reader.nextString());
                        }
                        reader.endArray();
                        model.tablePaths.put(key, path);
                    }
                    reader.endObject();
                }
                case "weights" -> {
                    reader.beginArray();
                    for (int i = 0; reader.hasNext(); i++) {
                        double w = reader.nextDouble();
                        if (i < model.ranker.weights.length) {
                            model.ranker.weights[i] = w;
                        }
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    @NotNull
    private static List<Map.Entry<String, SQLCompletionUsageModel.Frecency>> top(
        @NotNull Map<String, SQLCompletionUsageModel.Frecency> map,
        int limit,
        @NotNull SQLCompletionUsageModel model,
        long now
    ) {
        List<Map.Entry<String, SQLCompletionUsageModel.Frecency>> entries = new ArrayList<>(map.entrySet());
        if (entries.size() > limit) {
            double halfLife = model.getHalfLifeMs();
            entries.sort((e1, e2) -> Double.compare(e2.getValue().valueAt(now, halfLife), e1.getValue().valueAt(now, halfLife)));
            entries = entries.subList(0, limit);
        }
        return entries;
    }

    private static void writeFrecencies(
        @NotNull JsonWriter writer,
        @NotNull List<Map.Entry<String, SQLCompletionUsageModel.Frecency>> entries
    ) throws IOException {
        writer.beginObject();
        for (Map.Entry<String, SQLCompletionUsageModel.Frecency> e : entries) {
            writer.name(e.getKey()).beginArray().value(e.getValue().score).value(e.getValue().time).endArray();
        }
        writer.endObject();
    }

    private static void readFrecencies(
        @NotNull JsonReader reader,
        @NotNull Map<String, SQLCompletionUsageModel.Frecency> map
    ) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key = reader.nextName();
            SQLCompletionUsageModel.Frecency frecency = new SQLCompletionUsageModel.Frecency();
            reader.beginArray();
            frecency.score = reader.nextDouble();
            frecency.time = reader.nextLong();
            reader.endArray();
            map.put(key, frecency);
        }
        reader.endObject();
    }
}
