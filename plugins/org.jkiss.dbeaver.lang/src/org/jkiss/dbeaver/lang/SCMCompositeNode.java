/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.lang;

import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

/**
 * Source code node
 */
public interface SCMCompositeNode extends SCMNode {

    @NotNull
    SCMSourceText getSource();

    @NotNull
    List<SCMNode> getChildNodes();

    @Nullable
    SCMNode getFirstChild();

    @Nullable
    SCMNode getLastChild();

    @Nullable
    SCMNode getNextChild(SCMNode node);

    @Nullable
    SCMNode getPreviousChild(SCMNode node);

    void addChild(@NotNull SCMNode node);

    /**
     * Parse composite node contents.
     * Returns non-null token if trailing token can't be evaluated.
     */
    @Nullable
    IToken parseComposite(@NotNull SCMSourceScanner scanner);
}
