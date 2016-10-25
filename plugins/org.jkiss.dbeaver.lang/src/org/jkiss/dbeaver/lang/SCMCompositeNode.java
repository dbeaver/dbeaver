/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
