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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;

public class OAIMessageContent {

    public static final String TYPE_INPUT_TEXT = "input_text";
    public static final String TYPE_OUTPUT_TEXT = "output_text";


    public static final String TYPE_INPUT_IMAGE = "input_image";

    @SerializedName("image_url")
    public String imageUrl;
    public String type;
    public String text;
    public Object annotations;
    public Object logprobs;

    @NotNull
    public static OAIMessageContent image(@NotNull AIImageAttachment image) {
        OAIMessageContent content = new OAIMessageContent();
        content.type = TYPE_INPUT_IMAGE;
        content.imageUrl = image.toDataUrl();
        return content;
    }

    public OAIMessageContent() {
    }

    public OAIMessageContent(boolean isInput, @Nullable String text) {
        this.type = isInput ? TYPE_INPUT_TEXT : TYPE_OUTPUT_TEXT;
        this.text = text;
    }
}
