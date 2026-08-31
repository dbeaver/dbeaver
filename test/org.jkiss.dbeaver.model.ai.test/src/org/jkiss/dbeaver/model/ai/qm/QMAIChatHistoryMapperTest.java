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
package org.jkiss.dbeaver.model.ai.qm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIChatMessage;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QMAIChatHistoryMapperTest {

    @Test
    public void serializesFunctionResultWithoutTraversingValues() {
        AIFunctionResult result = new AIFunctionResult(
            AIFunctionType.INFORMATION,
            new CyclicValue(),
            null,
            new DBException("Database error")
        );
        AIMessage message = new AIMessage(
            new AIFunctionCall("test", Map.of()),
            result,
            LocalDateTime.now(),
            null
        );

        String json = QMAIChatHistoryMapper.toQMAIChatMessages(List.of(new AIChatMessage(1, message)))
            .getFirst()
            .functionResult();
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        Assertions.assertEquals("INFORMATION", object.get("type").getAsString());
        Assertions.assertEquals("Function result", object.get("value").getAsString());
        Assertions.assertEquals("Database error", object.get("exception").getAsString());
    }

    @Test
    public void restoresFunctionResultError() {
        AIFunctionResult result = QMAIChatHistoryMapper.fromFunctionResultJson(
            "{\"type\":\"INFORMATION\",\"value\":\"Failed\",\"exception\":{}}"
        );

        Assertions.assertEquals(AIFunctionType.INFORMATION, result.getType());
        Assertions.assertEquals("Failed", result.getValue());
        Assertions.assertNotNull(result.getException());
        Assertions.assertNull(result.getCallback());
    }

    private static class CyclicValue {
        @SuppressWarnings("unused")
        private final Object self = this;

        @Override
        public String toString() {
            return "Function result";
        }
    }
}
