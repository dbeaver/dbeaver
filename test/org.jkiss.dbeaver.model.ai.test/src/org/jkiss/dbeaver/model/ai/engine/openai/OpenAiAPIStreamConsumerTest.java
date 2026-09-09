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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.JsonSyntaxException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class OpenAiAPIStreamConsumerTest extends DBeaverUnitTest {
    private final AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
    private final OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener);

    @Test
    public void ignoresDoneMarker() {
        consumer.accept("data: [DONE]");
        consumer.accept("data:   [DONE]  ");

        Mockito.verifyNoInteractions(listener);
    }

    @Test
    public void preservesTextBeforeDoneMarker() {
        consumer.accept("data: {\"type\":\"response.output_text.delta\",\"delta\":\"There are 25 tables.\"}");
        consumer.accept("data: [DONE]");

        ArgumentCaptor<AIEngineResponseChunk> chunk = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener).nextChunk(chunk.capture());
        Assertions.assertEquals(List.of("There are 25 tables."), chunk.getValue().getChoices());
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void preservesFunctionCallBeforeDoneMarker() {
        consumer.accept("""
            data: {"type":"response.output_item.done","item":{"type":"function_call",\
            "name":"db_listTableNames","call_id":"call_1","arguments":"{\\"schemaNames\\":\\"dbo\\"}"}}
            """);
        consumer.accept("data: [DONE]");

        ArgumentCaptor<AIEngineResponseChunk> chunk = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener).nextChunk(chunk.capture());
        var functionCall = chunk.getValue().getFunctionCall();
        Assertions.assertNotNull(functionCall);
        Assertions.assertEquals("db_listTableNames", functionCall.getFunctionName());
        Assertions.assertEquals(Map.of("schemaNames", "dbo"), functionCall.getArguments());
        Assertions.assertEquals(Map.of("call_id", "call_1"), functionCall.getMessageMetadata());
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void reportsUnexpectedJsonArray() {
        consumer.accept("data: []");

        Mockito.verify(listener).error(Mockito.isA(JsonSyntaxException.class));
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void reportsApiError() {
        consumer.accept("data: {\"error\":{\"code\":\"invalid_request\",\"message\":\"Invalid model\"}}");

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(listener).error(error.capture());
        Assertions.assertInstanceOf(DBException.class, error.getValue());
        Assertions.assertEquals("invalid_request: Invalid model", error.getValue().getMessage());
        Mockito.verifyNoMoreInteractions(listener);
    }
}
