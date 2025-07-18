/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.model.ai.speech;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

public record TranscriptResult(String text, Usage usage) {

    public record Usage(Duration duration) {}

    public static class UsageAdapter extends TypeAdapter<Usage> {
        @Override
        public void write(JsonWriter out, Usage value) throws IOException {
            out.beginObject();
            out.name("seconds").value(value.duration().getSeconds());
            out.endObject();
        }

        @Override
        public Usage read(JsonReader in) throws IOException {
            Duration duration = null;
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if ("seconds".equals(name)) {
                    duration = Duration.ofSeconds(in.nextLong());
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            return new Usage(duration);
        }
    }
}