/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jkiss.utils.CommonUtils;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class JSONFormattingStrategy extends ContextBasedFormattingStrategy
{
    private ISourceViewer sourceViewer;
    private JSONSourceViewerConfiguration svConfig;

    JSONFormattingStrategy(ISourceViewer sourceViewer, JSONSourceViewerConfiguration svConfig)
    {
        this.sourceViewer = sourceViewer;
        this.svConfig = svConfig;
    }

    @Override
    public void formatterStarts(String initialIndentation)
    {
    }

    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions)
    {
        if (CommonUtils.isEmpty(content)) {
            return content;
        }
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(content);

        Gson gson = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setLenient()
            .create();
        String formattedJson = gson.toJson(jsonElement);
        return formattedJson;
    }

    @Override
    public void formatterStops()
    {
    }

}