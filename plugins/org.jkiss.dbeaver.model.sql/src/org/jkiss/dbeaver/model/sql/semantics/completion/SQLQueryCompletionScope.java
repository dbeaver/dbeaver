/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.RangeTransition;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser.SqlQueriesContext;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SyntaxParserTest;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class SQLQueryCompletionScope {
    
    public static final SQLQueryCompletionScope OFFQUERY = new SQLQueryCompletionScope(0) {
        private Collection<String> words = null;
        
        @Override
        protected Collection<String> getAvailableWordsAt(int position) {
            return this.words != null ? this.words : (this.words = SQLQueryKeywordsCompletion.prepareReservedWordsAtRoot());
        }
    };
    
    public static final SQLQueryCompletionScope EMPTY = new SQLQueryCompletionScope(0) {
        @Override
        protected Collection<String> getAvailableWordsAt(int position) {
            return Collections.emptyList();
        }
    };
    
    private final int delta;
    
    private SQLQueryCompletionScope(int delta) {
        this.delta = delta;
    }
    
    public SQLQueryCompletionProposal prepareProposalAt(int position) {
        position += this.delta;
        
//         return this.prepareProposalAtImpl(position);
        
        Collection<String> words = this.getAvailableWordsAt(position);
        
        Collection<SQLQueryCompletionItem> items = words.stream()
                .sorted()
                .map(s -> SQLQueryCompletionItem.forReservedWord(s))
                .collect(Collectors.toList());
                
        return new SQLQueryCompletionProposal(position, 0, items);
    }
    
//    private abstract SQLQueryCompletionProposal prepareProposalAtImpl(int position);
    
    protected abstract Collection<String> getAvailableWordsAt(int position);

    public static SQLQueryCompletionScope forKeywordsAt(SQLQueryNodeModel queryNodeModel, int delta) {
        return new SQLQueryNodeCompletionScope(queryNodeModel, delta);
    }
    
    private static class SQLQueryNodeCompletionScope extends SQLQueryCompletionScope {

        private final SQLQueryNodeModel model;

        public SQLQueryNodeCompletionScope(SQLQueryNodeModel model, int delta) {
            super(delta);
            this.model = model;
        }
            
        @Override
        protected Collection<String> getAvailableWordsAt(int position) {
            return SQLQueryKeywordsCompletion.forKeywordsAt(this.model, position);
        }
    }
    
    
}