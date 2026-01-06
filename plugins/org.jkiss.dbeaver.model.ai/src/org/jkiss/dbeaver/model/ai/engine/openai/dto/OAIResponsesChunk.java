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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Object containing a response chunk from the chat completions streaming api.
 */
public class OAIResponsesChunk {

    public String type;
    @SerializedName("sequence_number")
    public Integer sequenceNumber;
    public OAIResponsesResponse response;
    public OAIMessage item;
    @SerializedName("item_id")
    public String itemId;
    @SerializedName("output_index")
    public int outputIndex;
    @SerializedName("content_index")
    public int contentIndex;
    public String delta;

}