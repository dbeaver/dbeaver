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
 * Standard implementation of <code>TPToken</code>.
 */
public class TPTokenAbstract<DATA> implements TPToken {

    private static final int T_UNDEFINED    = 0;
    private static final int T_EOF          = 1;
    private static final int T_WHITESPACE   = 2;
    static final int T_OTHER        = 3;

    public static final TPToken UNDEFINED = new TPTokenAbstract(T_UNDEFINED);
    public static final TPToken EOF = new TPTokenAbstract(T_EOF);
    public static final TPToken WHITESPACE = new TPTokenAbstract(T_WHITESPACE);

    /**
     * The type of this token
     */
    private int type;
    private DATA data;

    private TPTokenAbstract(int type) {
        this.type = type;
    }

    /**
     * Creates a new token according to the given specification which does not
     * have any data attached to it.
     */
    protected TPTokenAbstract(int type, DATA data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public DATA getData() {
        return data;
    }

    @Override
    public boolean isOther() {
        return (type == T_OTHER);
    }

    @Override
    public boolean isEOF() {
        return (type == T_EOF);
    }

    @Override
    public boolean isWhitespace() {
        return (type == T_WHITESPACE);
    }

    @Override
    public boolean isUndefined() {
        return (type == T_UNDEFINED);
    }

}
