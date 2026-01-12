/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsp;

import org.jkiss.code.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentURI {
    private static final Pattern URI_PATTERN = Pattern.compile("^lsp://([A-Za-z0-9_-]+)/(\\S.*)$");

    @NotNull
    private final String uri;
    @NotNull
    private final String projectId;
    @NotNull
    private final String resourcePath;

    public DocumentURI(@NotNull String uri) {
        Matcher matcher = URI_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid URI format: %s, expected lsp://{projectId}/{resourcePath}", uri)
            );
        }
        this.uri = uri;
        this.projectId = matcher.group(1);
        this.resourcePath = matcher.group(2);
    }

    @NotNull
    public String getProjectId() {
        return projectId;
    }

    @NotNull
    public String getResourcePath() {
        return resourcePath;
    }

    @NotNull
    public String getValue() {
        return uri;
    }

    @Override
    public String toString() {
        return uri;
    }
}
