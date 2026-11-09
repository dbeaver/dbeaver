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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIChatMessage;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.internal.AIChatMessages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AIChatTimeUtilsTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    public void formatsTwelveHourTimeWithPeriodAndTwentyFourHourTimeWithoutIt() {
        LocalDateTime time = TODAY.atTime(14, 15);
        Assertions.assertTrue(AIChatTimeUtils.format(time, TODAY, Locale.US, true).endsWith("PM"));
        Assertions.assertEquals(AIChatMessages.ai_chat_date_today + ", 14:15",
            AIChatTimeUtils.format(time, TODAY, Locale.UK, true));
        Assertions.assertTrue(AIChatTimeUtils.format(TODAY.atTime(9, 5), TODAY, Locale.US, true).endsWith("AM"));
    }

    @Test
    public void messagesTodayShowOnlyTimeAndYesterdayUsesItsDateLabel() {
        Assertions.assertEquals("14:15", AIChatTimeUtils.format(TODAY.atTime(14, 15), TODAY, Locale.UK, false));
        Assertions.assertEquals(AIChatMessages.ai_chat_date_yesterday + ", 09:05",
            AIChatTimeUtils.format(TODAY.minusDays(1).atTime(9, 5), TODAY, Locale.UK, false));
    }

    @Test
    public void conversationTimestampUsesItsFirstMessageRatherThanItsLatestMessage() {
        LocalDateTime created = TODAY.minusDays(1).atTime(9, 5);
        LocalDateTime latest = TODAY.atTime(14, 15);
        AIChatConversation conversation = new AIChatConversation(UUID.randomUUID(), "Chat", Mockito.mock(AIPromptGenerator.class),
            List.of(new AIChatMessage(1, new AIMessage(AIMessageType.USER, "First", null, created, null)),
                new AIChatMessage(2, new AIMessage(AIMessageType.ASSISTANT, "Last", null, latest, null))), null, 3, null);
        Assertions.assertEquals(created, conversation.getTime());
        Assertions.assertEquals(latest, conversation.getLastMessageTime());
    }

    @Test
    public void addsYearForOlderYearAndDoesNotConfuseSameDayOfYear() {
        Assertions.assertEquals("3 Nov, 16:40",
            AIChatTimeUtils.format(LocalDateTime.of(2026, 11, 3, 16, 40), TODAY, Locale.UK, true));
        Assertions.assertEquals("6 Nov 2025, 22:30",
            AIChatTimeUtils.format(LocalDateTime.of(2025, 11, 6, 22, 30), TODAY, Locale.UK, false));
    }
}
