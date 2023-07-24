/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.dpi.model.client;

import org.jkiss.code.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigUtils {

    public static void storeProperties(BufferedWriter bw, @NotNull Map<String, String> properties) throws IOException {
        for (Map.Entry<String, String> e : properties.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            bw.write(key + "=" + val);
            bw.newLine();
        }
        bw.flush();
    }

    @NotNull
    public static Map<String, String> readPropertiesFromFile(Path serverConfigFile) throws IOException {
        Map<String, String> props = new LinkedHashMap<>();
        try (BufferedReader br = Files.newBufferedReader(serverConfigFile)) {
            br.lines().forEach(s -> {
                String[] lineValue = s.split("=");
                if (lineValue.length == 2) {
                    props.put(lineValue[0], lineValue[1]);
                }
            });
        }
        return props;
    }
}
