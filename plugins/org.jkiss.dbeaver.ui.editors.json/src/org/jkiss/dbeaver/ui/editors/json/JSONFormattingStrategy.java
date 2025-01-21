/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.json;

import com.google.gson.*;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jkiss.utils.CommonUtils;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class JSONFormattingStrategy extends ContextBasedFormattingStrategy {
    private ISourceViewer sourceViewer;
    private JSONSourceViewerConfiguration svConfig;

    public static Gson GSON_FORMATTED = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT)
            .create();

    public static Gson GSON_UNFORMATTED = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setStrictness(Strictness.LENIENT)
        .create();

    JSONFormattingStrategy(ISourceViewer sourceViewer, JSONSourceViewerConfiguration svConfig) {
        this.sourceViewer = sourceViewer;
        this.svConfig = svConfig;
    }

    @Override
    public void formatterStarts(String initialIndentation) {
    }

    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        if (CommonUtils.isEmpty(content)) {
            return content;
        }
        JsonElement jsonElement = JsonParser.parseString(content);

        return GSON_FORMATTED.toJson(jsonElement);
    }

    @Override
    public void formatterStops() {
    }

}