/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.websocket;

import com.google.gson.*;
import org.jkiss.dbeaver.model.websocket.event.WSEvent;
import org.jkiss.dbeaver.model.websocket.event.WSEventType;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class WSEventDeserializer implements JsonDeserializer<WSEvent> {
    private static final String EVENT_ID_FIELD = "id";
    private final Gson gson = new Gson();
    private final Map<String, Class<? extends WSEvent>> eventClassById =
        Arrays.stream(WSEventType.values())
            .collect(Collectors.toMap(
                WSEventType::getEventId,
                WSEventType::getEventClass
            ));

    @Override
    public WSEvent deserialize(
        JsonElement jsonElement,
        Type type,
        JsonDeserializationContext jsonDeserializationContext
    ) throws JsonParseException {
        var jsonObject = jsonElement.getAsJsonObject();
        var eventIdElement = jsonObject.get(EVENT_ID_FIELD);
        if (eventIdElement == null) {
            throw new JsonParseException("Event id not present: " + jsonElement);
        }
        var resultClass = eventClassById.get(eventIdElement.getAsString());
        if (resultClass == null) {
            throw new JsonParseException("Unknown event: " + eventIdElement);
        }
        return gson.fromJson(jsonElement, resultClass);
    }
}
