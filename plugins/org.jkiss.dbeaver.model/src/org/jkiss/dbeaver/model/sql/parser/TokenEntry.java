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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Represents an information about the token in the text.
 * Any member can be null if represents partial information in case of placeholder.
 */
public interface TokenEntry {
    /**
     * @return text fragment corresponding to the token
     */
    @Nullable
    String getString();

    /**
     * @return token type
     */
    @Nullable
    Enum getTokenType();

    /**
     * Checks if two entries could possibly describe the same concrete token
     * @param other
     * @return true if entries could  describe the same concrete token
     */
    boolean matches(@NotNull TokenEntry other);
}
