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
package org.jkiss.dbeaver.model.text.parser;


/**
 * Replica of ICharacterScanner but without jface/swt
 */
public interface TPCharacterScanner {

    /**
     * The value returned when this scanner has read EOF.
     */
    int EOF = -1;

    /**
     * Provides rules access to the legal line delimiters. The returned
     * object may not be modified by clients.
     *
     * @return the legal line delimiters
     */
    char[][] getLegalLineDelimiters();

    /**
     * Returns the column of the character scanner.
     *
     * @return the column of the character scanner
     */
    int getColumn();

    /**
     * Returns the next character or EOF if end of file has been reached
     *
     * @return the next character or EOF
     */
    int read();

    /**
     * Rewinds the scanner before the last read character.
     */
    void unread();
}
