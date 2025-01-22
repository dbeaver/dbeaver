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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a query model part in the source text. Connects model with syntax tree and text region.
 */
public abstract class SQLQueryNodeModel {

    @NotNull
    private final Interval region;
    @NotNull
    private final STMTreeNode syntaxNode;
    @Nullable
    private List<SQLQueryNodeModel> subnodes; // TODO validate that subnodes are being registered correctly for all nodes
    @Nullable
    private List<SQLQueryLexicalScope> lexicalScopes  = null;
    @Nullable
    private SQLQuerySymbolOrigin tailOrigin = null;
    
    protected SQLQueryNodeModel(@NotNull Interval region, @NotNull STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel ... subnodes) {
        this.region = region;
        this.syntaxNode = syntaxNode;
        
        if (subnodes == null || subnodes.length == 0) {
            this.subnodes = null;
        } else {
            this.subnodes = Stream.of(subnodes)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new ArrayList<>(subnodes.length)));
            this.subnodes.sort(Comparator.comparingInt(n -> n.region.a));
        }
    }

    protected void setTailOrigin(SQLQuerySymbolOrigin tailOrigin) {
        this.tailOrigin = tailOrigin;
    }

    @Nullable
    public SQLQuerySymbolOrigin getTailOrigin() {
        return this.tailOrigin;
    }

    /**
     * Register lexical scopes, if they haven't been registered yet
     */
    public void registerLexicalScope(@NotNull SQLQueryLexicalScope lexicalScope) {
        List<SQLQueryLexicalScope> scopes = this.lexicalScopes;
        if (scopes == null) {
            this.lexicalScopes = scopes = new ArrayList<>();
        }
        scopes.add(lexicalScope);
    }

    /**
     * Returns lexical scope for the text part in the corresponding position
     */
    public SQLQueryLexicalScope findLexicalScope(int position) {
        List<SQLQueryLexicalScope> scopes = this.lexicalScopes;
        if (scopes != null) {
            for (SQLQueryLexicalScope s : scopes) {
                Interval region = s.getInterval();
                if (region.a <= position && region.b >= position) {
                    return s;
                }
            }
        }
        
        return null;
    }

    protected void registerSubnode(@NotNull SQLQueryNodeModel subnode) {
        this.subnodes = STMUtils.orderedInsert(this.subnodes, n -> n.region.a, subnode, Comparator.comparingInt(x -> x));
    }  

    @NotNull
    public final Interval getInterval() {
        return this.region;
    }
    
    @NotNull
    public final STMTreeNode getSyntaxNode() {
        return this.syntaxNode;
    }

    /**
     * Apply the visitor
     */
    public final <T, R> R apply(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg);
    
    protected SQLQueryNodeModel findChildNodeContaining(int position) { // TODO check it
        if (this.subnodes != null) {
            if (this.subnodes.size() == 1) {
                SQLQueryNodeModel node = this.subnodes.get(0);
                return node.region.a <= position && node.region.b >= position - 1 ? node : null;
            } else {
                int index = STMUtils.binarySearchByKey(this.subnodes, n -> n.region.a, position, Comparator.comparingInt(x -> x));
                if (index >= 0) {
                    SQLQueryNodeModel node = this.subnodes.get(index);
                    int i = index + 1;
                    while (i < this.subnodes.size()) {
                        SQLQueryNodeModel next = this.subnodes.get(i++);
                        if (next.region.a > position - 1) {
                            break;
                        } else {
                            node = next;
                            i++;   
                        }
                    }
                    return node;
                } else {
                    for (int i = ~index - 1; i >= 0; i--) {
                        SQLQueryNodeModel node = this.subnodes.get(i);
                        if (node.region.a <= position && node.region.b >= position - 1) {
                            return node;
                        } else if (node.region.b < position) {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get initial data context
     */
    @Nullable
    public abstract SQLQueryDataContext getGivenDataContext();

    /**
     * Get result data context
     */
    @Nullable
    public abstract SQLQueryDataContext getResultDataContext();

    /**
     * Debugging stuff
     */
    public String collectScopesHierarchyDebugView() {
        interface ITextBlock {
            Interval getInterval();

            String prepareText(int widthToFill);
        }

        class ColumnInfo {
            public final int position;
            public int width = 0;

            public ColumnInfo(int position) {
                this.position = position;
            }
        }

        class TextBlocksColumnsMap {
            private final TreeMap<Integer, ColumnInfo> columns = new TreeMap<>();
            
            public TextBlocksColumnsMap() {
                this.columns.put(0, new ColumnInfo(0));
            }

            public void register(int position) {
                this.columns.put(position, new ColumnInfo(position));
            }
        }

        class TextBlockInfo {
            public final ITextBlock block;
            public final int contentWidth;
            public NavigableMap<Integer, ColumnInfo> columns = null;
            
            public TextBlockInfo(ITextBlock block, int contentWidth) {
                this.block = block;
                this.contentWidth = contentWidth;
            }
        }

        class TextBlocksLine {
            private final List<TextBlockInfo> blocks = new ArrayList<>();
            
            public void add(ITextBlock block, int width) {
                this.blocks.add(new TextBlockInfo(block, width));
            }
            
            public void collectColumns(TextBlocksColumnsMap columns) {
                for (var b : this.blocks) {
                    Interval r = b.block.getInterval();
                    columns.register(r.a); // 7
                    columns.register((r.b + r.a) / 2);
                    columns.register(r.b + 1); // 8
                    b.columns = columns.columns.subMap(r.a, true, r.b + 1, false); // [7,8)
                    System.out.println(r + " : " +  b.columns.size());
                }
            }
            
            public void adjustComlumns() {
                for (var b : this.blocks) {
                    // TODO b.columns.values().stream().mapToInt(c -> c.width).sum() >= b.contentWidth
                    var currWidth = b.columns.values().stream().mapToInt(c -> c.width).sum();
                    var delta = b.contentWidth - currWidth;
                    if (delta > 0) {
                        System.out.println("adjusting " + currWidth + " to " + b.contentWidth);
                        var eqstep = delta / b.columns.size();
                        var rest = b.contentWidth;
                        for (var c : b.columns.values()) {
                            if (c.width < eqstep) {
                                c.width = eqstep;
                                System.out.println("  column " + c.position + " for " + c.width);
                            }
                            rest -= c.width;
                        }
                        if (rest > 0) {
                            b.columns.lastEntry().getValue().width += rest;
                            System.out.println("  +rest " + rest);
                        }
                    }
//                    if (range.a == pos) {
//                        column += 1;
//                    } else if (range.a > pos){
//                        column += 2;
//                    } else {
//                        throw new IllegalStateException();
//                    }
                }
            }

            public void collectContents(StringBuilder sb, TextBlocksColumnsMap columns) {
                sb.append("|");
                var currColumn = columns.columns.firstEntry().getValue();
                for (var b : this.blocks) {
                    var headColumnPos = b.columns.firstEntry().getValue().position;
                    if (currColumn.position < headColumnPos) {
                        int indent = columns.columns.subMap(0, true, headColumnPos, false).values().stream().mapToInt(c -> c.width).sum();
                        sb.append(" ".repeat(indent));
                        sb.append("|");
                    }
                    int width = b.columns.values().stream().mapToInt(c -> c.width).sum();
                    sb.append(b.block.prepareText(width));
                    sb.append("|");    
                    currColumn = b.columns.lastEntry().getValue();
                }
            }
        }

        class TextBlocks {
            private final List<TextBlocksLine> lines = new ArrayList<>();
            
            public String getContents() {
                TextBlocksColumnsMap columnsMap = new TextBlocksColumnsMap();
                for (var l : this.lines) {
                    l.collectColumns(columnsMap);
                }
                for (var l : this.lines) {
                    l.adjustComlumns();
                }
                StringBuilder sb = new StringBuilder();
                for (var l : this.lines) {
                    l.collectContents(sb, columnsMap);
                    sb.append(System.lineSeparator());
                }
                return sb.toString();
            }
            
            public TextBlocksLine appendLine() {
                TextBlocksLine line = new TextBlocksLine();
                this.lines.add(line);
                return line;
            }
        }

        class Range implements ITextBlock {
            public final String label;
            public final Interval interval;
            public final List<List<Range>> subranges = new ArrayList<>();
            
            public Range(String label, Interval interval) {
                this.label = label;
                this.interval = interval;
            }
            
            public Interval getInterval() {
                return this.interval;
            }
            
            public String prepareText(int widthToFill) {
                String a = Integer.toString(this.interval.a);
                String b = Integer.toString(this.interval.b);
                int minWidth = a.length() + 1 + this.label.length() + 1 + b.length();
                int space = Math.max(0,  widthToFill - minWidth); 
                int space1 = space / 2;
                int space2 = space - space1;
                return a +
                    " ".repeat(1 + space1) +
                    this.label +
                    " ".repeat(1 + space2) +
                    b;
            }
            
            public List<Range> createSubrangesLayer() {
                List<Range> ranges = new ArrayList<>();
                this.subranges.add(ranges);
                return ranges;
            }

            public int collectText(TextBlocks text) {
                return this.collectTextInternal(text, text.appendLine());
            }

            private int collectTextInternal(TextBlocks text, TextBlocksLine line) {
                int width = this.prepareText(0).length();
                for (List<Range> rr : this.subranges) {
                    TextBlocksLine l = text.appendLine();
                    int lineWidth = rr.size() - 1;
                    for (Range r : rr) {
                        lineWidth += r.collectTextInternal(text, l);
                    }
                    width = Math.max(lineWidth, width);
                }
                line.add(this, width);
                return width;
            }
        }
        
        var local = new Object() {
            public Range collectModel(SQLQueryNodeModel node) {
                if (node.getGivenDataContext() != null) {
                    var range = new Range(
                        node.getClass().getSimpleName() + ": " + node.getGivenDataContext().getClass().getSimpleName(),
                        node.getInterval()
                    );
                    if (node.lexicalScopes != null && node.lexicalScopes.size() > 0) {
                        List<Range> layer = range.createSubrangesLayer();
                        for (var s : node.lexicalScopes) {
                            SQLQuerySymbolOrigin origin = s.getSymbolsOrigin();
                            if (origin != null) {
                                String originName = origin.getClass().getSimpleName();
                                String contextName = origin instanceof SQLQuerySymbolOrigin.DataContextSymbolOrigin o
                                    ? "(" + o.getDataContext().getClass().getSimpleName() + ")"
                                    : "";
                                layer.add(new Range("lexical: " + originName + contextName, s.getInterval()));
                            }
                        }
                    }
                    if (node.subnodes != null && node.subnodes.size() > 0) {
                        List<Range> layer = range.createSubrangesLayer();
                        for (var n : node.subnodes) {
                            layer.add(collectModel(n));
                        }
                    }
                    return range;
                }
                return null;
            }
        };
        
        Range root = local.collectModel(this);
        
        TextBlocks text = new TextBlocks();
        if (root != null) {
            root.collectText(text);
        }
        
        return text.getContents();
    }
}
