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
package org.jkiss.dbeaver.registry.project;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProjectImportTransformer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBInterruptedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectImportRegistry {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.projectImport";

    private final List<TransformerDescriptor> transformers = new ArrayList<>();

    public ProjectImportRegistry() {
        for (IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            transformers.add(new TransformerDescriptor(element));
        }
    }

    /**
     * Unhandled resources remain streamed. The caller owns both the supplied and returned streams.
     * Only resources with registered text transformers are decoded; unchanged text retains its original bytes.
     *
     * @param charset charset selected by the caller for decoding and encoding the resource
     * @throws IOException if reading fails or the text cannot be decoded or encoded without losing data
     */
    @NotNull
    public InputStream transformResource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path relativePath,
        @NotNull InputStream contents,
        @NotNull Charset charset,
        @NotNull Map<String, String> projectNames
    ) throws DBException, IOException {
        var applicableTransformers = transformers.stream()
            .filter(descriptor -> descriptor.path.equals(relativePath))
            .toList();
        if (applicableTransformers.isEmpty()) {
            return contents;
        }
        if (monitor.isCanceled()) {
            throw new DBInterruptedException("Project import canceled");
        }
        var originalBytes = contents.readAllBytes();
        var originalText = charset.newDecoder().decode(ByteBuffer.wrap(originalBytes)).toString();
        var transformed = originalText;
        for (TransformerDescriptor descriptor : applicableTransformers) {
            if (monitor.isCanceled()) {
                throw new DBInterruptedException("Project import canceled");
            }
            transformed = descriptor.type
                .createInstance(DBPProjectImportTransformer.class)
                .transform(monitor, transformed, projectNames);
        }
        if (originalText.equals(transformed)) {
            return new ByteArrayInputStream(originalBytes);
        }
        var encoded = charset.newEncoder().encode(CharBuffer.wrap(transformed));
        var transformedBytes = new byte[encoded.remaining()];
        encoded.get(transformedBytes);
        return new ByteArrayInputStream(transformedBytes);
    }

    private static class TransformerDescriptor extends AbstractDescriptor {
        private final Path path;
        private final ObjectType type;

        private TransformerDescriptor(@NotNull IConfigurationElement config) {
            super(config);
            path = Path.of(config.getAttribute("path"));
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        }
    }
}
