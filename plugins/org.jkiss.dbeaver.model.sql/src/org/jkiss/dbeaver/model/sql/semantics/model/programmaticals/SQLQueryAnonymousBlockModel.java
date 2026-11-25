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
package org.jkiss.dbeaver.model.sql.semantics.model.programmaticals;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/** Anonymous DO / EXECUTE BLOCK, possibly with an unfinished body. */
public final class SQLQueryAnonymousBlockModel extends SQLQueryProgrammaticalBodyStatementModel {
    private SQLQueryAnonymousBlockModel(@NotNull STMTreeNode node, @Nullable SQLQueryStatementModel body) {
        super(node, body);
    }

    @NotNull
    public static SQLQueryAnonymousBlockModel recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode bodyNode = null;
        for (STMTreeNode child : node.findNonErrorChildren()) {
            if (child.getNodeKindId() == SQLStandardParser.RULE_proceduralBlock ||
                child.getNodeKindId() == SQLStandardParser.RULE_firebirdBlock ||
                child.getNodeKindId() == SQLStandardParser.RULE_dollarQuotedBody ||
                child.getNodeKindId() == SQLStandardParser.RULE_characterStringLiteral) {
                bodyNode = child;
                break;
            }
        }
        return new SQLQueryAnonymousBlockModel(node,
            bodyNode == null ? null : SQLQueryStatementModel.recognize(recognizer, bodyNode));
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitAnonymousBlock(this, arg);
    }
}
