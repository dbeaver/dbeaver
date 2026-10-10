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
package org.jkiss.dbeaver.maven.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductGeneratorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void generatesProductAndCopiesLauncherResources() throws Exception {
        Path source = tempDirectory.resolve("source");
        Path output = tempDirectory.resolve("target/generated-products");
        Files.createDirectories(source.resolve("icons"));
        Files.writeString(source.resolve("icons/test.ico"), "icon", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("test.p2.inf"), "instructions.configure=test");
        Files.writeString(source.resolve("test.product"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <product>
                <launcherArgs>
                    <vmArgs>
                        <!-- dbeaver-launch-parameters: common -->
                        -Xmx1g
                    </vmArgs>
                </launcherArgs>
                <launcher><win useIco="true"><ico path="icons/test.ico"/></win></launcher>
            </product>
            """, StandardCharsets.UTF_8);

        int count = new ProductGenerator(getClass().getClassLoader()).generate(
            source,
            output,
            tempDirectory.resolve("target")
        );

        String generated = Files.readString(output.resolve("test.product"));
        assertEquals(1, count);
        assertTrue(generated.contains("--enable-native-access=ALL-UNNAMED"));
        assertTrue(generated.contains("            --enable-native-access=ALL-UNNAMED"));
        assertTrue(generated.contains("-Xmx1g"));
        assertFalse(generated.contains("dbeaver-launch-parameters"));
        assertTrue(Files.isRegularFile(output.resolve("icons/test.ico")));
        assertEquals("instructions.configure=test", Files.readString(output.resolve("test.p2.inf")));
    }

    @Test
    void rejectsUnknownParameterSet() throws Exception {
        Path source = tempDirectory.resolve("source");
        Files.createDirectories(source);
        Files.writeString(source.resolve("test.product"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <product><launcherArgs><vmArgs>
                <!-- dbeaver-launch-parameters: missing -->
            </vmArgs></launcherArgs></product>
            """, StandardCharsets.UTF_8);

        ProductGenerationException exception = assertThrows(
            ProductGenerationException.class,
            () -> new ProductGenerator(getClass().getClassLoader()).generate(
                source,
                tempDirectory.resolve("target/generated-products"),
                tempDirectory.resolve("target")
            )
        );
        assertTrue(exception.getMessage().contains("Unknown launch parameter set 'missing'"));
    }

    @Test
    void rejectsMarkerOutsideVmArgs() throws Exception {
        Path source = tempDirectory.resolve("source");
        Files.createDirectories(source);
        Files.writeString(source.resolve("test.product"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <product><!-- dbeaver-launch-parameters: common --></product>
            """, StandardCharsets.UTF_8);

        ProductGenerationException exception = assertThrows(
            ProductGenerationException.class,
            () -> new ProductGenerator(getClass().getClassLoader()).generate(
                source,
                tempDirectory.resolve("target/generated-products"),
                tempDirectory.resolve("target")
            )
        );
        assertTrue(exception.getMessage().contains("direct child of <vmArgs>"));
    }
}
