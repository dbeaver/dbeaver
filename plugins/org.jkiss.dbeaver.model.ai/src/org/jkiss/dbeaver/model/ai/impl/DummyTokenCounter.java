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
package org.jkiss.dbeaver.model.ai.impl;

public class DummyTokenCounter implements TokenCounter {
    public static final int TOKEN_TO_CHAR_RATIO = 2;

    @Override
    public int count(String message) {
        if (message.isEmpty()) {
            return 0;
        }

        return message.length() / TOKEN_TO_CHAR_RATIO;
    }

    @Override
    public String truncateToTokenLimit(String message, int tokenLimit) {
        if (count(message) <= tokenLimit) {
            return message;
        }
        int charLimit = tokenLimit * TOKEN_TO_CHAR_RATIO;
        return message.substring(0, Math.min(charLimit, message.length()));
    }
}
