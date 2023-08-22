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
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jkiss.code.NotNull;

public abstract class STMParserOverrides extends Parser {

    public STMParserOverrides(@NotNull TokenStream input) {
        super(input);
        this.setBuildParseTree(true);
    }

    @NotNull
    @Override
    public ErrorNode createErrorNode(@NotNull ParserRuleContext parent, @NotNull Token t) {
        return new STMTreeTermErrorNode(t);
    }

    @NotNull
    @Override
    public TerminalNode createTerminalNode(@NotNull ParserRuleContext parent, @NotNull Token t) {
        return new STMTreeTermNode(t);
    }

}
