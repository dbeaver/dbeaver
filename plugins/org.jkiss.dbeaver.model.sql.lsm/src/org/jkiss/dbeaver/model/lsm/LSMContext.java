/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsm;

public class LSMContext {

    private final String parserName;
    private final LSMLexer lexer;
    private final LSMParser parser;

    public LSMContext(String parserName, LSMLexer lexer, LSMParser parser) {
        this.parserName = parserName;
        this.lexer = lexer;
        this.parser = parser;
    }

    public String getParserName() {
        return parserName;
    }

    public LSMLexer getLexer() {
        return lexer;
    }

    public LSMParser getParser() {
        return parser;
    }
}
