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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.NotNullWhen;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StringTemplate {

    private static final Pattern DEFAULT_PARAM_VALUE_PATTERN = Pattern.compile("[\\d\\w\\-\\.\\~\\%\\s\\_]+");

    private enum TermKind {
        TEXT(null),
        ESCAPE_MARK("\\"),
        PARAM_A_START("{"),
        PARAM_A_END("}"),
        PARAM_B_START("<"),
        PARAM_B_END(">"),
        GROUP_NAME_SEPARATOR(":"),
        ALTERNATIVE("|"),
        REPEAT_MARK("..."),
        OPTIONAL_START("["),
        OPTIONAL_END("]");

        @Nullable
        public final String text;

        private static final Map<String, TermKind> kindByKnownText = Arrays.stream(TermKind.values())
            .filter(k -> k.text != null)
            .collect(Collectors.toMap(k -> k.text, Function.identity()));

        TermKind(@Nullable String text) {
            this.text = text;
        }
    }

    private static final String NAMED_TERMS_REGEX = Arrays.stream(TermKind.values())
        .filter(k -> k.text != null)
        .map(k -> Pattern.quote(k.text))
        .collect(Collectors.joining(")|(", "((", "))"));

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(NAMED_TERMS_REGEX);

    private record Term(
        @NotNull String text,
        @NotNull TermKind kind,
        int start
    ) {
        public int end() {
            return this.start + this.text.length();
        }
    }

    private static class Lexer {
        private final String text;
        private final Matcher matcher;
        private int pos = 0;
        private ListNode<Term> nextTerm = null;
        private Term lastTerm = null;

        public Lexer(@NotNull String template) {
            this.text = template;
            this.matcher = TEMPLATE_PATTERN.matcher(template);
        }

        public void pushBack(@NotNull Term t) {
            this.nextTerm = ListNode.push(this.nextTerm, t);
        }

        @Nullable
        public Term nextTerm() {
            Term result;
            if (this.nextTerm != null) {
                result = this.nextTerm.data;
                this.nextTerm = this.nextTerm.next;
            } else if (this.matcher.find(this.pos)) {
                String fragment = this.text.substring(this.matcher.start(), this.matcher.end());
                TermKind kind = TermKind.kindByKnownText.get(fragment);
                if (kind == TermKind.ESCAPE_MARK && this.matcher.end() < this.text.length()) {
                    fragment = this.text.substring(this.matcher.start(), this.matcher.end() + 1);
                }
                Term t = new Term(fragment, kind, this.matcher.start());
                if (this.matcher.start() > this.pos) {
                    this.pushBack(t);
                    result = new Term(this.text.substring(this.pos, this.matcher.start()), TermKind.TEXT, this.pos);
                } else {
                    result = t;
                }
                this.pos = this.matcher.start() + fragment.length();
            } else {
                if (this.pos < this.text.length()) {
                    result = new Term(this.text.substring(this.pos), TermKind.TEXT, this.pos);
                    this.pos = this.text.length();
                } else {
                    result = null;
                }
            }
            if (result != null) {
                this.lastTerm = result;
            }
            return result;
        }
    }

    private enum TemplateSyntaxNodeKind {
        ERROR,
        TEXT,
        PARAM,
        OPTIONAL,
        REPEAT,
        ALTERNATIVE
    }

    private record TemplateSyntaxNode(
        TemplateSyntaxNodeKind kind,
        int start,
        int end,
        @Nullable String payload,
        @NotNullWhen(
            "kind == TemplateSyntaxNodeKind.OPTIONAL || kind == TemplateSyntaxNodeKind.REPEAT || kind == TemplateSyntaxNodeKind.ALTERNATIVE"
        ) List<TemplateSyntaxNode> children
    ) {
    }

    private enum TemplateFragmentKind {
        DEFAULT,
        OPTIONAL,
        SUBSEQUENCE
    }

    @NotNull
    private static List<TemplateSyntaxNode> parseTemplate(
        @NotNull Lexer l, @NotNull TemplateFragmentKind fragmentKind
    ) throws StringTemplateFormatException {
        int fragmentStart = l.lastTerm == null ? 0 : l.lastTerm.start();
        List<TemplateSyntaxNode> nodes = new ArrayList<>();
        Term t = l.nextTerm();
        while (t != null) {
            switch (t.kind) {
                case TEXT, GROUP_NAME_SEPARATOR, ESCAPE_MARK -> {
                    String text = t.kind == TermKind.ESCAPE_MARK ? t.text.substring(1) : t.text;
                    boolean joined = false;
                    if (!nodes.isEmpty()) {
                        TemplateSyntaxNode lastNode = nodes.getLast();
                        if (lastNode.kind  == TemplateSyntaxNodeKind.TEXT && lastNode.end() == t.start()) {
                            nodes.set(nodes.size() - 1, new TemplateSyntaxNode(
                                TemplateSyntaxNodeKind.TEXT,
                                lastNode.start(),
                                t.end(),
                                lastNode.payload + text,
                                null
                            ));
                            joined = true;
                        }
                    }
                    if (!joined) {
                        nodes.add(new TemplateSyntaxNode(TemplateSyntaxNodeKind.TEXT, t.start(), t.end(), text, null));
                    }
                }
                case PARAM_A_START -> nodes.add(parseParamOrSubseq(l, t, TermKind.PARAM_A_END));
                case PARAM_A_END -> {
                    if (fragmentKind == TemplateFragmentKind.SUBSEQUENCE) {
                        if (nodes.getFirst().kind == TemplateSyntaxNodeKind.ALTERNATIVE) {
                            if (nodes.size() > 1) {
                                TemplateSyntaxNode lastBranch = new TemplateSyntaxNode(
                                    TemplateSyntaxNodeKind.TEXT,
                                    nodes.get(1).start(),
                                    t.start(),
                                    null,
                                    List.copyOf(nodes.subList(1, nodes.size()))
                                );
                                if (nodes.getFirst().children != null) {
                                    nodes.getFirst().children.add(lastBranch);
                                }
                            }
                            return List.of(nodes.getFirst());
                        } else {
                            return nodes;
                        }
                    } else {
                        throw new StringTemplateFormatException(l.text, t.start(), "Unexpected parameter end mark '}' at " + t.start());
                    }
                }
                case PARAM_B_START -> nodes.add(parseParamNode(l, t, TermKind.PARAM_B_END));
                case PARAM_B_END ->
                    throw new StringTemplateFormatException(l.text, t.start(), "Unexpected parameter end mark '>' at " + t.start());
                case ALTERNATIVE -> {
                    int contentsStart;
                    if (nodes.getFirst().kind != TemplateSyntaxNodeKind.ALTERNATIVE) {
                        contentsStart = 0;
                    } else {
                        contentsStart = 1;
                    }
                    TemplateSyntaxNode branch = new TemplateSyntaxNode(
                        TemplateSyntaxNodeKind.TEXT,
                        nodes.get(contentsStart).start(),
                        t.start(),
                        null,
                        List.copyOf(nodes.subList(contentsStart, nodes.size()))
                    );
                    int lastIndex = nodes.size();
                    List<TemplateSyntaxNode> branches;
                    while (lastIndex-- > contentsStart) {
                        nodes.remove(lastIndex);
                    }
                    if (contentsStart == 0) {
                        branches = new ArrayList<>();
                        nodes.add(new TemplateSyntaxNode(
                            TemplateSyntaxNodeKind.ALTERNATIVE,
                            branch.start(),
                            Integer.MAX_VALUE,
                            null,
                            branches
                        ));
                    } else {
                        branches = nodes.getFirst().children();
                    }
                    if (branches != null) {
                        branches.add(branch);
                    }
                }
                case REPEAT_MARK -> {
                    t = l.nextTerm();
                    if (t != null) {
                        if (t.kind == TermKind.OPTIONAL_END && fragmentKind == TemplateFragmentKind.OPTIONAL) {
                            return List.of(
                                new TemplateSyntaxNode(
                                    TemplateSyntaxNodeKind.REPEAT,
                                    nodes.getFirst().start(),
                                    l.lastTerm.end(),
                                    null,
                                    nodes
                                )
                            );
                        } else {
                            // consider allowing it in subsequence
                            throw new StringTemplateFormatException(l.text, t.start(), "Unexpected repeat mark '...' at " + t.start());
                        }
                    }
                }
                case OPTIONAL_START -> {
                    List<TemplateSyntaxNode> children = parseTemplate(l, TemplateFragmentKind.OPTIONAL);
                    nodes.add(new TemplateSyntaxNode(TemplateSyntaxNodeKind.OPTIONAL, t.start(), l.lastTerm.end(), null, children));
                }
                case OPTIONAL_END -> {
                    if (fragmentKind == TemplateFragmentKind.OPTIONAL) {
                        if (nodes.getFirst().kind == TemplateSyntaxNodeKind.ALTERNATIVE) {
                            if (nodes.size() > 1) {
                                TemplateSyntaxNode lastBranch = new TemplateSyntaxNode(
                                    TemplateSyntaxNodeKind.TEXT,
                                    nodes.get(1).start(),
                                    t.start(),
                                    null,
                                    List.copyOf(nodes.subList(1, nodes.size()))
                                );
                                if (nodes.getFirst().children != null) {
                                    nodes.getFirst().children.add(lastBranch);
                                }
                            }
                            return List.of(nodes.getFirst());
                        } else {
                            return nodes;
                        }
                    } else {
                        throw new StringTemplateFormatException(l.text, t.start(), "Unexpected optional end mark ']' at " + t.start());
                    }
                }
                default -> {
                    throw new StringTemplateFormatException(l.text, t.start(), "Unexpected '" + t.text() + "' syntax at " + t.start());
                }
            }
            t = l.nextTerm();
        }
        if (fragmentKind == TemplateFragmentKind.DEFAULT) {
            return nodes;
        } else {
            throw new StringTemplateFormatException(l.text, fragmentStart, "Unclosed " + fragmentKind + " pattern at " + fragmentStart);
        }
    }

    @NotNull
    private static TemplateSyntaxNode parseParamNode(
        @NotNull Lexer l, @NotNull Term t, @NotNull TermKind endTermKind
    ) throws StringTemplateFormatException {
        Term t2;
        do {
            t2 = l.nextTerm();
        } while (t2 != null && t2.kind != endTermKind);

        if (t2 != null) {
            String name = l.text.substring(t.start() + 1, t2.end() - 1);
            return new TemplateSyntaxNode(TemplateSyntaxNodeKind.PARAM, t.start(), t2.end(), name, null);
        } else {
            throw new StringTemplateFormatException(l.text, t.start(), "Unfinished parameter pattern at " + t.start());
        }
    }

    @NotNull
    private static TemplateSyntaxNode parseParamOrSubseq(
        @NotNull Lexer l, @NotNull Term t, @NotNull TermKind endTermKind
    ) throws StringTemplateFormatException {
        Term nameTerm = l.nextTerm();
        String groupName = null;
        if (nameTerm != null) {
            if (nameTerm.kind == TermKind.TEXT) {
                Term paramEndTerm = l.nextTerm();
                if (paramEndTerm != null) {
                    String name = l.text.substring(t.start() + 1, paramEndTerm.end() - 1);
                    if (paramEndTerm.kind == endTermKind) {
                        return new TemplateSyntaxNode(TemplateSyntaxNodeKind.PARAM, t.start(), paramEndTerm.end(), name, null);
                    } else if (paramEndTerm.kind == TermKind.GROUP_NAME_SEPARATOR) {
                        groupName = name;
                    } else {
                        // fallthrough
                    }
                } else {
                    throw new StringTemplateFormatException(l.text, t.start(), "Unfinished parameter pattern at " + t.start());
                }
                if (groupName == null) {
                    l.pushBack(paramEndTerm);
                }
            } else {
                // fallthrough
            }
        } else {
            throw new StringTemplateFormatException(l.text, t.start(), "Unfinished parameter pattern at " + t.start());
        }
        if (groupName == null) {
            l.pushBack(nameTerm);
        }

        List<TemplateSyntaxNode> nodes = parseTemplate(l, TemplateFragmentKind.SUBSEQUENCE);
        return new TemplateSyntaxNode(TemplateSyntaxNodeKind.TEXT, nodes.getFirst().start(), nodes.getLast().end(), groupName, nodes);
    }

    private interface TemplateNodeVisitor<T, R> {

        @Nullable
        R visitSequence(@NotNull TemplateNode.Sequence sequence, @NotNull T arg);

        @Nullable
        R visitText(@NotNull TemplateNode.Text text, @NotNull T arg);

        @Nullable
        R visitOptional(@NotNull TemplateNode.Optional optional, @NotNull T arg);

        @Nullable
        R visitParameter(@NotNull TemplateNode.Parameter parameter, @NotNull T arg);

        @Nullable
        R visitAlternatives(@NotNull TemplateNode.Alternatives alternatives, @NotNull T arg);

        @Nullable
        R visitRepeat(@NotNull TemplateNode.Repeat repeat, @NotNull T arg);
    }

    private abstract static class TemplateNode {

        @Nullable
        public abstract <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg);

        @NotNull
        public String collectDebugView() {
            StringBuilder sb = new StringBuilder();
            this.visit(new TemplateNodeVisitor<Integer, Object>() {
                @Nullable
                Object append(@NotNull TemplateNode node, @NotNull Integer depth) {
                    sb.append("\n");
                    sb.repeat("  ", Math.max(0, depth));
                    sb.append(node.getClass().getSimpleName()).append(": ").append(node.toTemplateString());
                    return null;
                }

                @Nullable
                Object visit(@NotNull TemplateNode node, @NotNull Integer depth) {
                    node.visit(this, depth + 1);
                    return null;
                }

                @Nullable
                @Override
                public Object visitSequence(@NotNull Sequence sequence, @NotNull Integer arg) {
                    this.append(sequence, arg);
                    sequence.children.forEach(n -> this.visit(n, arg));
                    return null;
                }

                @Nullable
                @Override
                public Object visitText(@NotNull Text text, @NotNull Integer arg) {
                    return this.append(text, arg);
                }

                @Nullable
                @Override
                public Object visitOptional(@NotNull Optional optional, @NotNull Integer arg) {
                    this.append(optional, arg);
                    return this.visit(optional.child, arg);
                }

                @Nullable
                @Override
                public Object visitParameter(@NotNull Parameter parameter, @NotNull Integer arg) {
                    return this.append(parameter, arg);
                }

                @Nullable
                @Override
                public Object visitAlternatives(@NotNull Alternatives alternatives, @NotNull Integer arg) {
                    this.append(alternatives, arg);
                    alternatives.branches.forEach(n -> this.visit(n, arg));
                    return null;
                }

                @Nullable
                @Override
                public Object visitRepeat(@NotNull Repeat repeat, @NotNull Integer arg) {
                    this.append(repeat, arg);
                    return this.visit(repeat.child, arg);
                }
            }, 0);
            return sb.toString();
        }

        @NotNull
        private String toTemplateString() {
            String result = this.visit(new TemplateNodeVisitor<TemplateNode, String>() {

                @NotNull
                @Override
                public String visitSequence(@NotNull Sequence sequence, @NotNull TemplateNode parent) {
                    String result = sequence.children.stream()
                        .map(n -> n.visit(this, sequence)).collect(Collectors.joining(""));
                    if (sequence.name != null) {
                        result  = "{" + sequence.name + ":" + result + "}";
                    }
                    return result;
                }

                @NotNull
                @Override
                public String visitText(@NotNull Text text, @NotNull TemplateNode parent) {
                    return text.text;
                }

                @NotNull
                @Override
                public String visitOptional(@NotNull Optional optional, @NotNull TemplateNode parent) {
                    return "[" + optional.child.visit(this, optional) + "]";
                }

                @NotNull
                @Override
                public String visitParameter(@NotNull Parameter parameter, @NotNull TemplateNode parent) {
                    return "{" + parameter.name + "}";
                }

                @Nullable
                @Override
                public String visitAlternatives(@NotNull Alternatives alternatives, @NotNull TemplateNode parent) {
                    var collector = parent instanceof Optional
                        ? Collectors.joining("|")
                        : Collectors.joining("|", "{", "}");
                    return alternatives.branches.stream().map(b -> b.visit(this, alternatives)).collect(collector);
                }

                @NotNull
                @Override
                public String visitRepeat(@NotNull Repeat repeat, @NotNull TemplateNode parent) {
                    return repeat.child.visit(this, repeat) + " ...";
                }
            }, null);

            if (result == null) {
                // never happens, because this visitor never returns null
                throw new IllegalStateException("Failed to prepare template string");
            }

            return result;
        }

        @NotNull
        @Override
        public String toString() {
            String str = this.toTemplateString();
            return super.toString() + "[\"" + str.replace("\"", "\\\"") + "\"]";
        }

        public static class Sequence extends TemplateNode {
            @Nullable
            public final String name;
            @NotNull
            public final List<TemplateNode> children;

            public Sequence(@Nullable String name, @NotNull List<TemplateNode> children) {
                this.name = name;
                this.children = children;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitSequence(this, arg);
            }
        }

        public static class Text extends TemplateNode {
            @NotNull
            public final String text;

            public Text(@NotNull String text) {
                this.text = text;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitText(this, arg);
            }
        }

        public static class Optional extends TemplateNode {

            public final boolean hasParameters;
            @NotNull
            public final TemplateNode child;

            public Optional(boolean hasParameters, @NotNull TemplateNode child) {
                this.hasParameters = hasParameters;
                this.child = child;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitOptional(this, arg);
            }
        }

        public static class Parameter extends TemplateNode {
            @NotNull
            public final String name;

            public Parameter(@NotNull String name) {
                this.name = name;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitParameter(this, arg);
            }
        }

        public static class Alternatives extends TemplateNode {
            @NotNull
            public final List<TemplateNode> branches;

            public Alternatives(@NotNull List<TemplateNode> branches) {
                this.branches = branches;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitAlternatives(this, arg);
            }
        }

        public static class Repeat extends TemplateNode {
            @NotNull
            public final TemplateNode child;

            public Repeat(@NotNull TemplateNode child) {
                this.child = child;
            }

            @Nullable
            @Override
            public <T, R> R visit(@NotNull TemplateNodeVisitor<T, R> visitor, @NotNull T arg) {
                return visitor.visitRepeat(this, arg);
            }
        }
    }

    public record GroupInfo(
        @NotNull String name,
        @NotNull Map<String, GroupInfo> subgroups,
        @NotNull Set<String> parametersNames
    ) {
    }

    public record ParameterInfo(
        @NotNull String name,
        boolean isSingleton,
        boolean isMandatory,
        int totalCases
    ) {
    }

    @NotNull
    private final String templateString;
    @NotNull
    private final TemplateNode root;
    @NotNull
    private final Map<String, ParameterInfo> paramInfoByName;
    @NotNull
    private final GroupInfo groupsInfo;

    private final boolean isNotPlain;
    @NotNull
    private final Pattern pattern;

    private StringTemplate(
        @NotNull String templateString,
        @NotNull TemplateNode root,
        @Nullable IParameterPatternSupplier parameterPatternSupplier
    ) {
        this.templateString = templateString;
        this.root = root;

        Pair<TemplateEvalInfo, GroupEvalInfo> templateInfo = collectTemplateInfo(root);
        this.paramInfoByName = Collections.unmodifiableMap(templateInfo.getFirst().params());
        this.groupsInfo = templateInfo.getSecond().toGroupInfo("");
        this.isNotPlain = templateInfo.getFirst().hasNamedGroups || this.paramInfoByName.values().stream().anyMatch(p -> !p.isSingleton);
        this.pattern = prepareRegexPattern(root, this.paramInfoByName, parameterPatternSupplier);
    }

    @NotNull
    public Map<String, ParameterInfo> getParametersInfo() {
        return this.paramInfoByName;
    }

    @NotNull
    public GroupInfo getGroupsInfo() {
        return this.groupsInfo;
    }

    private record TemplateEvalInfo(
        boolean hasNamedGroups,
        @NotNull Map<String, ParameterInfo> params
    ) {
        @NotNull
        public static final TemplateEvalInfo EMPTY = new TemplateEvalInfo(false, Collections.emptyMap());

        @NotNull
        public static TemplateEvalInfo ofParameter(@NotNull String paramName) {
            return new TemplateEvalInfo(false, Map.of(paramName, new ParameterInfo(paramName, true, true, 1)));
        }

        @NotNull
        public static TemplateEvalInfo ofOptional(@NotNull TemplateEvalInfo info) {
            return new TemplateEvalInfo(
                info.hasNamedGroups,
                info.params().values().stream().collect(Collectors.toMap(
                    p -> p.name, p -> !p.isMandatory ? p : new ParameterInfo(p.name, p.isSingleton, false, p.totalCases)
                ))
            );
        }

        @NotNull
        public static TemplateEvalInfo ofRepeatable(@NotNull TemplateEvalInfo info) {
            return new TemplateEvalInfo(
                info.hasNamedGroups,
                info.params().values().stream().collect(Collectors.toMap(
                    p -> p.name, p -> !p.isSingleton ? p : new ParameterInfo(p.name, false, p.isMandatory, p.totalCases)
                ))
            );
        }

        @NotNull
        public static TemplateEvalInfo and(@NotNull List<TemplateEvalInfo> info) {
            return new TemplateEvalInfo(
                info.stream().anyMatch(e -> e.hasNamedGroups),
                info.stream().flatMap(e -> e.params().keySet().stream()).distinct().map(k -> {
                    int minCount = 0;
                    int maxCount = 0;
                    int totalCases = 0;
                    for (TemplateEvalInfo e : info) {
                        ParameterInfo p = e.params().get(k);
                        if (p != null) {
                            minCount += p.isMandatory ? 1 : 0;
                            maxCount += p.isSingleton ? 1 : 2;
                            totalCases += p.totalCases;
                        }
                    }
                    return new ParameterInfo(k, maxCount < 2, minCount > 0, totalCases);
                }).collect(Collectors.toMap(p -> p.name, Function.identity()))
            );
        }

        @NotNull
        public static TemplateEvalInfo or(@NotNull List<TemplateEvalInfo> info) {
            return new TemplateEvalInfo(
                info.stream().anyMatch(e -> e.hasNamedGroups),
                info.stream().flatMap(e -> e.params().keySet().stream()).distinct().map(k -> {
                    int minCount = Integer.MAX_VALUE;
                    int maxCount = Integer.MIN_VALUE;
                    int totalCases = 0;
                    for (TemplateEvalInfo e : info) {
                        ParameterInfo p = e.params().get(k);
                        if (p != null) {
                            minCount = Math.min(minCount, p.isMandatory ? 1 : 0);
                            maxCount = Math.max(maxCount, p.isSingleton ? 1 : 2);
                            totalCases += p.totalCases;
                        } else {
                            minCount = 0;
                        }
                    }
                    return new ParameterInfo(k, maxCount < 2, minCount > 0, totalCases);
                }).collect(Collectors.toMap(p -> p.name, Function.identity()))
            );
        }
    }

    private static class GroupEvalInfo {
        @Nullable
        private Map<String, GroupEvalInfo> subgroups = null;
        @Nullable
        private Set<String> params = null;

        @NotNull
        public GroupEvalInfo getOrRegisterGroup(@NotNull String name) {
            if (this.subgroups == null) {
                this.subgroups = new HashMap<>();
            }
            return this.subgroups.computeIfAbsent(name, k -> new GroupEvalInfo());
        }

        public void registerParam(@NotNull String name) {
            if (this.params == null) {
                this.params = new HashSet<>();
            }
            this.params.add(name);
        }

        @NotNull
        public GroupInfo toGroupInfo(@NotNull String name) {
            return new GroupInfo(
                name,
                this.subgroups == null ? Collections.emptyMap() : this.subgroups.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, e -> e.getValue().toGroupInfo(e.getKey())
                )),
                this.params == null ? Collections.emptySet() : Collections.unmodifiableSet(this.params)
            );
        }
    }

    @NotNull
    private static Pair<TemplateEvalInfo, GroupEvalInfo> collectTemplateInfo(@NotNull TemplateNode root) {
        Stack<GroupEvalInfo> groups = new Stack<>();
        groups.push(new GroupEvalInfo());

        TemplateEvalInfo result = root.visit(new TemplateNodeVisitor<Object, TemplateEvalInfo>() {

            @NotNull
            TemplateEvalInfo visit(@NotNull  TemplateNode node, @NotNull Object arg) {
                TemplateEvalInfo info = node.visit(this, arg);
                if (info == null) {
                    throw new IllegalStateException(); // never happens because this visitor doesn't return null
                }
                return info;
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitSequence(@NotNull TemplateNode.Sequence sequence, @NotNull Object arg) {
                if (sequence.name != null) {
                    groups.push(groups.peek().getOrRegisterGroup(sequence.name));
                }
                TemplateEvalInfo result = TemplateEvalInfo.and(sequence.children.stream().map(n -> this.visit(n, arg)).toList());
                if (sequence.name != null) {
                    groups.pop();
                }
                return result;
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitText(@NotNull TemplateNode.Text text, @NotNull Object arg) {
                return TemplateEvalInfo.EMPTY;
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitOptional(@NotNull TemplateNode.Optional optional, @NotNull Object arg) {
                TemplateEvalInfo evalInfo = this.visit(optional.child, arg);
                return TemplateEvalInfo.ofOptional(evalInfo);
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitParameter(@NotNull TemplateNode.Parameter parameter, @NotNull Object arg) {
                groups.peek().registerParam(parameter.name);
                return TemplateEvalInfo.ofParameter(parameter.name);
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitAlternatives(@NotNull TemplateNode.Alternatives alternatives, @NotNull Object arg) {
                return TemplateEvalInfo.or(alternatives.branches.stream().map(n -> this.visit(n, arg)).toList());
            }

            @NotNull
            @Override
            public TemplateEvalInfo visitRepeat(@NotNull TemplateNode.Repeat repeat, @NotNull Object arg) {
                TemplateEvalInfo evalInfo = this.visit(repeat.child, arg);
                return TemplateEvalInfo.ofRepeatable(evalInfo);
            }
        }, "");

        return Pair.of(result, groups.peek());
    }

    @NotNull
    private static Pattern prepareRegexPattern(
        @NotNull TemplateNode root,
        @NotNull Map<String, ParameterInfo> paramInfoByName,
        @Nullable IParameterPatternSupplier parameterPatternSupplier
    ) {
        Fragment regex = root.visit(new TemplateNodeVisitor<Object, Fragment>() {
            @Nullable
            private Map<String, AtomicInteger> paramEntryCounters = null;

            int nextEntryNumber(@NotNull String paramName) {
                if (this.paramEntryCounters == null) {
                    this.paramEntryCounters = new HashMap<>();
                }
                return this.paramEntryCounters.computeIfAbsent(paramName, k -> new AtomicInteger(0)).getAndIncrement();
            }

            @NotNull
            Fragment visit(@NotNull  TemplateNode node, @NotNull Object arg) {
                Fragment info = node.visit(this, arg);
                if (info == null) {
                    throw new IllegalStateException(); // never happens because this visitor doesn't return null
                }
                return info;
            }

            @NotNull
            @Override
            public Fragment visitSequence(@NotNull TemplateNode.Sequence sequence, @NotNull Object arg) {
                LinkedList<Fragment> pieces = new LinkedList<>();
                pieces.addLast(Fragment.ofText("("));
                for (TemplateNode node : sequence.children) {
                    pieces.addLast(this.visit(node, arg));
                }
                pieces.addLast(Fragment.ofText(")"));
                return Fragment.ofSequence(pieces);
            }

            @NotNull
            @Override
            public Fragment visitText(@NotNull TemplateNode.Text text, @NotNull Object arg) {
                return Fragment.ofSequence(List.of(
                    Fragment.ofText("("),
                    Fragment.ofText(Pattern.quote(text.text)),
                    Fragment.ofText(")")
                ));
            }

            @NotNull
            @Override
            public Fragment visitOptional(@NotNull TemplateNode.Optional optional, @NotNull Object arg) {
                return Fragment.ofSequence(List.of(
                    Fragment.ofText("("),
                    this.visit(optional.child, arg),
                    Fragment.ofText(")?")
                ));
            }

            @NotNull
            @Override
            public Fragment visitParameter(@NotNull TemplateNode.Parameter parameter, @NotNull Object arg) {
                ParameterInfo paramInfo = paramInfoByName.get(parameter.name);
                String valueRegex = parameterPatternSupplier != null
                    ? parameterPatternSupplier.getParamRegex(paramInfo)
                    : DEFAULT_PARAM_VALUE_PATTERN.pattern();
                return Fragment.ofSequence(
                    paramInfo.isSingleton
                        ? List.of(
                            Fragment.ofText("(?<"),
                            Fragment.ofText(parameter.name + (paramInfo.totalCases <= 1 ? "" : this.nextEntryNumber(paramInfo.name))),
                            Fragment.ofText(">"),
                            Fragment.ofText(valueRegex),
                            Fragment.ofText(")")
                        )
                        : List.of(
                            Fragment.ofText("("),
                            Fragment.ofText(valueRegex),
                            Fragment.ofText(")")
                        )
                );
            }

            @NotNull
            @Override
            public Fragment visitAlternatives(@NotNull TemplateNode.Alternatives alternatives, @NotNull Object arg) {
                LinkedList<Fragment> pieces = new LinkedList<>();
                pieces.addLast(Fragment.ofText("("));
                for (int i = 0; i < alternatives.branches.size(); i++) {
                    TemplateNode node = alternatives.branches.get(i);
                    if (i > 0) {
                        pieces.addLast(Fragment.ofText("|"));
                    }
                    pieces.addLast(this.visit(node, arg));
                }
                pieces.addLast(Fragment.ofText(")"));
                return Fragment.ofSequence(pieces);
            }

            @NotNull
            @Override
            public Fragment visitRepeat(@NotNull TemplateNode.Repeat repeat, @NotNull Object arg) {
                return Fragment.ofSequence(List.of(
                    Fragment.ofText("("),
                    Objects.requireNonNull(this.visit(repeat.child, arg)),
                    Fragment.ofText(")+")
                ));
            }
        }, "");

        if (regex == null) {
            // never happens, because this visitor never returns null
            throw new IllegalStateException("Can't prepare pattern");
        }
        String regexString = regex.collectString();
        return Pattern.compile("^" + regexString);
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + "[\"" + this.templateString.replace("\"", "\\\"") + "\"]";
    }

    public static class ParamEntries {
        @Nullable
        private Map<String, List<String>> parameters = null;
        @Nullable
        private Map<String, List<ParamEntries>> groups = null;

        @Nullable
        public String getFirstParamValue(@NotNull String paramName) {
            return this.parameters == null ? null : this.parameters.get(paramName).getFirst();
        }

        @NotNull
        public Map<String, List<String>> getParameters() {
            return this.parameters != null ? this.parameters : Collections.emptyMap();
        }

        @NotNull
        public Map<String, List<ParamEntries>> getGroups() {
            return this.groups != null ? this.groups : Collections.emptyMap();
        }

        private void putParam(@NotNull String paramName, @NotNull String value) {
            if (this.parameters == null) {
                this.parameters = new HashMap<>();
            }
            this.parameters.computeIfAbsent(paramName, k -> new ArrayList<>()).add(value);
        }

        @NotNull
        private ParamEntries newBranch(@NotNull String groupName) {
            if (this.groups == null) {
                this.groups = new HashMap<>();
            }
            ParamEntries branch = new ParamEntries();
            this.groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(branch);
            return branch;
        }

        @NotNull
        public Map<String, ?> toMap() {
            Map<String, Object> result = new HashMap<>();
            if (this.parameters != null) {
                result.putAll(this.parameters);
            }
            if (this.groups != null) {
                for (Map.Entry<String, List<ParamEntries>> g : this.groups.entrySet()) {
                    result.put(g.getKey(), g.getValue().stream().map(ParamEntries::toMap).toList());
                }
            }
            return result;
        }

        @NotNull
        public String collectDebugView() {
            StringBuilder sb = new StringBuilder();
            this.collectDebugView(sb, 0);
            return sb.toString();
        }

        private void collectDebugView(@NotNull StringBuilder sb, int depth) {
            if (this.parameters != null) {
                for (Map.Entry<String, List<String>> kv : this.parameters.entrySet()) {
                    for (String value : kv.getValue()) {
                        sb.repeat("  ", depth).append(kv.getKey()).append(": ").append(value).append("\n");
                    }
                }
            }
            if (this.groups != null) {
                for (Map.Entry<String, List<ParamEntries>> kv : this.groups.entrySet()) {
                    for (ParamEntries ee : kv.getValue()) {
                        sb.repeat("  ", depth).append(kv.getKey()).append(":\n");
                        ee.collectDebugView(sb, depth + 1);
                    }
                }
            }
        }
    }

    @Nullable
    public ParamEntries extractAllParametersTree(@NotNull String string) {
        CapturesEnumerator it = this.extractAllParametersImpl(string);
        if (it != null) {
            ListNode<ParamEntries> stack = ListNode.of(new ParamEntries());
            while (it.nextCapture()) {
                switch (it.getCaptureKind()) {
                    case PARAMETER_VALUE -> stack.data.putParam(it.getParamName(), it.getPayload());
                    case GROUP_START -> stack = ListNode.push(stack, stack.data.newBranch(it.getPayload()));
                    case GROUP_END -> stack = stack.next;
                    default -> throw new UnsupportedOperationException();
                }
            }
            return stack.data;
        } else {
            return null;
        }
    }

    @Nullable
    public Map<String, String> extractSingletonParametersMap(@NotNull String string) {
        CapturesEnumerator it = this.extractAllParametersImpl(string);
        if (it != null) {
            Map<String, String> result = new HashMap<>();
            while (it.nextCapture()) {
                if (it.captureKind == CaptureKind.PARAMETER_VALUE) {
                    result.put(it.getParamName(), it.getPayload());
                }
            }
            return result;
        } else {
            return null;
        }
    }

    @Nullable
    public Map<String, List<String>> extractAllParametersMap(@NotNull String string) {
        CapturesEnumerator it = this.extractAllParametersImpl(string);
        if (it != null) {
            Map<String, List<String>> result = new HashMap<>();
            while (it.nextCapture()) {
                if (it.getCaptureKind() == CaptureKind.PARAMETER_VALUE) {
                    result.computeIfAbsent(it.getParamName(), k -> new ArrayList<>()).add(it.getPayload());
                }
            }
            return result;
        } else {
            return null;
        }
    }

    @Nullable
    public List<Map.Entry<String, String>> extractAllParametersFlat(@NotNull String string) {
        CapturesEnumerator it = this.extractAllParametersImpl(string);
        if (it != null) {
            ArrayList<Map.Entry<String, String>> result = new ArrayList<>();
            while (it.nextCapture()) {
                if (it.getCaptureKind() == CaptureKind.PARAMETER_VALUE) {
                    result.add(Map.entry(it.getParamName(), it.getPayload()));
                }
            }
            return result;
        } else {
            return null;
        }
    }

    @Nullable
    private CapturesEnumerator extractAllParametersImpl(@NotNull String text) {
        if (this.isNotPlain) {
            TreeMatchStep path = this.applyTreeMatch(text);
            if (path != null) {
                return new CapturesEnumerator() {
                    private TreeMatchStep step = path;

                    public boolean nextCapture() {
                        while (this.step != null) {
                            switch (this.step) {
                                case TreeMatchStep.Parameter p -> {
                                    this.captureKind = CaptureKind.PARAMETER_VALUE;
                                    this.currParamName = p.paramName();
                                    this.currPayload = text.substring(p.prevPosition(), p.position());
                                }
                                case TreeMatchStep.EndGroup g when g.groupName != null -> {
                                    this.captureKind = CaptureKind.GROUP_START;
                                    this.currParamName = null;
                                    this.currPayload = g.groupName;
                                }
                                case TreeMatchStep.BeginGroup g when g.groupName != null -> {
                                    this.captureKind = CaptureKind.GROUP_END;
                                    this.currParamName = null;
                                    this.currPayload = g.groupName;
                                }
                                default -> this.captureKind = null;
                            }
                            this.step = this.step.prev();
                            if (this.captureKind != null) {
                                return true;
                            }
                        }
                        this.captureKind = null;
                        this.currParamName = null;
                        this.currPayload = null;
                        return false;
                    }
                };
            } else {
                return null;
            }
        } else {
            Matcher m = this.pattern.matcher(text);

            if (m.find()) {
                return new CapturesEnumerator() {
                    private final Iterator<ParameterInfo> parameters = paramInfoByName.values().iterator();

                    @Override
                    public boolean nextCapture() {
                        while (this.parameters.hasNext()) {
                            ParameterInfo p = this.parameters.next();
                            if (p.isSingleton) {
                                this.captureKind = CaptureKind.PARAMETER_VALUE;
                                this.currParamName = p.name;
                                if (p.totalCases > 1) {
                                    for (int i = 0; i < p.totalCases; i++) {
                                        String value = m.group(p.name + i);
                                        if (value != null) {
                                            this.currPayload = value;
                                            return true;
                                        }
                                    }
                                } else {
                                    String value = m.group(p.name);
                                    if (value != null) {
                                        this.currPayload = value;
                                        return true;
                                    }
                                }
                            }
                        }
                        return false;
                    }
                };
            } else {
                return null;
            }
        }
    }

    private enum CaptureKind {
        PARAMETER_VALUE,
        GROUP_START,
        GROUP_END
    }

    private abstract static class CapturesEnumerator {

        protected CaptureKind captureKind = null;
        protected String currParamName = null;
        protected String currPayload = null;

        @NotNullWhen("nextCapture()")
        public CaptureKind getCaptureKind() {
            return this.captureKind;
        }

        @NotNullWhen("getCaptureKind() == CaptureKind.PARAMETER_VALUE")
        public String getParamName() {
            return this.currParamName;
        }

        @NotNullWhen("nextCapture()")
        public String getPayload() {
            return this.currPayload;
        }

        public abstract boolean nextCapture();
    }

    private interface TreeMatchStep {

        @Nullable
        TreeMatchStep prev();

        default int position() {
            TreeMatchStep prev = this.prev();
            return prev != null ? prev.position() : 0;
        }

        default int prevPosition() {
            TreeMatchStep prev = this.prev();
            return prev == null ? 0 : prev.position();
        }

        record Text(@Nullable TreeMatchStep prev, int position)  implements TreeMatchStep {
        }

        record Parameter(@Nullable TreeMatchStep prev, int position, String paramName) implements TreeMatchStep {
        }

        record BeginGroup(@Nullable TreeMatchStep prev, @Nullable String groupName) implements TreeMatchStep {
        }

        record EndGroup(@Nullable TreeMatchStep prev, String groupName) implements TreeMatchStep {
        }
    }

    @Nullable
    private TreeMatchStep applyTreeMatch(@NotNull String string) {

        TreeMatchStep path = this.root.visit(new TemplateNodeVisitor<TreeMatchStep, TreeMatchStep>() {
            @Nullable
            @Override
            public TreeMatchStep visitSequence(@NotNull TemplateNode.Sequence sequence, @NotNull TreeMatchStep step) {
                TreeMatchStep current = sequence.name == null ? step : new TreeMatchStep.BeginGroup(step, sequence.name);
                for (TemplateNode node : sequence.children) {
                    current = node.visit(this, current);
                    if (current == null) {
                        return null;
                    }
                }
                return sequence.name == null ? current : new TreeMatchStep.EndGroup(current, sequence.name);
            }

            @Nullable
            @Override
            public TreeMatchStep visitText(@NotNull TemplateNode.Text text, @NotNull TreeMatchStep step) {
                if (string.regionMatches(step.position(), text.text, 0, text.text.length())) {
                    return new TreeMatchStep.Text(step, step.position() + text.text.length());
                } else {
                    return null;
                }
            }

            @NotNull
            @Override
            public TreeMatchStep visitOptional(@NotNull TemplateNode.Optional optional, @NotNull TreeMatchStep step) {
                TreeMatchStep next = optional.child.visit(this, step);
                return next != null ? next : step;
            }

            @Nullable
            @Override
            public TreeMatchStep visitParameter(@NotNull TemplateNode.Parameter parameter, @NotNull TreeMatchStep step) {
                Matcher m = DEFAULT_PARAM_VALUE_PATTERN.matcher(string);
                if (m.find(step.position()) && m.start() == step.position()) {
                    return new TreeMatchStep.Parameter(step, m.end(), parameter.name);
                } else {
                    return null;
                }
            }

            @Nullable
            @Override
            public TreeMatchStep visitAlternatives(@NotNull TemplateNode.Alternatives alternatives, @NotNull TreeMatchStep step) {
                for (TemplateNode branch : alternatives.branches) {
                    TreeMatchStep dst = branch.visit(this, step);
                    if (dst != null) {
                        return dst;
                    }
                }
                return null;
            }

            @Nullable
            @Override
            public TreeMatchStep visitRepeat(@NotNull TemplateNode.Repeat repeat, @NotNull TreeMatchStep step) {
                TreeMatchStep prev = null;
                TreeMatchStep current = repeat.child.visit(this, step);
                while (current != null) {
                    prev = current;
                    current = repeat.child.visit(this, current);
                }
                return prev;
            }
        }, new TreeMatchStep.BeginGroup(null, null));

        return path;
    }

    @NotNull
    private ParameterSource prepareParameterSource(@NotNull Map<String, ?> parameters) {
        ParameterSource result = new ParameterSource();

        for (Map.Entry<String, ?> kv : parameters.entrySet()) {
            switch (kv.getValue()) {
                case String s -> result.addParameter(kv.getKey(), ClonableEnumerator.ofValue(s));
                case List<?> l when l.isEmpty() || l.getFirst() instanceof String ->
                    result.addParameter(kv.getKey(), ClonableEnumerator.ofList((List<String>) l));
                case List<?> l when !l.isEmpty() && l.getFirst() instanceof Map ->
                    result.addGroup(kv.getKey(), ClonableEnumerator.ofList(
                        l.stream().map(m -> this.prepareParameterSource((Map<String, ?>) m)).collect(Collectors.toList())
                    ));
                case Map<?, ?> m when !m.isEmpty() ->
                    result.addGroup(kv.getKey(), ClonableEnumerator.ofValue(this.prepareParameterSource((Map<String, ?>) m)));
                default -> throw new IllegalArgumentException("Map value should be either String or List<String> or Map<String, ?>");
            }
        }

        return result;
    }

    @NotNull
    public String prepareString(@NotNull Map<String, ?> parameters) {
        return this.prepareStringImpl(this.prepareParameterSource(parameters));
    }

    @NotNull
    public String prepareString(@NotNull Collection<Map.Entry<String, String>> parameters) {
        // it used to be
        //     Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(e -> e.getValue(), Collectors.toCollection(LinkedList::new)))
        // but we want custom finisher to turn the resulting group collection into clonable-enumerator
        Collector<String, LinkedList<String>, ClonableEnumerator<String>> groupCollector = Collector.of(
            LinkedList::new,
            LinkedList::add,
            (r1, r2) -> {
                r1.addAll(r2);
                return r1;
            },
            ClonableEnumerator::ofList
        );

        return this.prepareStringImpl(new ParameterSource(parameters.stream().collect(
            Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, groupCollector))
        )));
    }
    
    private static class ParameterSource {
        @Nullable
        private Map<String, ClonableEnumerator<String>> parameters;
        @Nullable
        private Map<String, ClonableEnumerator<ParameterSource>> groups;

        public ParameterSource() {
            this.parameters = null;
            this.groups = null;
        }

        public ParameterSource(@NotNull Map<String, ClonableEnumerator<String>> parameters) {
            this.parameters = parameters;
            this.groups = null;
        }

        private ParameterSource(@NotNull ParameterSource source) {
            this.parameters = source.parameters == null ? null : source.parameters.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone())
            );
            this.groups = source.groups == null ? null : source.groups.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> e.getValue().clone())
            );
        }

        @Nullable
        public ClonableEnumerator<String> findParameter(@NotNull  String name) {
            return this.parameters == null ? null : this.parameters.get(name);
        }

        @Nullable
        public ClonableEnumerator<ParameterSource> findGroup(@NotNull String name) {
            return this.groups == null ? null : this.groups.get(name);
        }

        public boolean containsAllParameters(@NotNull Set<String> requiredParamNames) {
            return this.parameters != null && this.parameters.keySet().containsAll(requiredParamNames);
        }

        public void updateFrom(@NotNull ParameterSource other) {
            this.parameters = other.parameters;
            this.groups = other.groups;
        }

        @Override
        @NotNull
        public ParameterSource clone() {
            return new ParameterSource(this);
        }

        public void addParameter(@NotNull String key, @NotNull ClonableEnumerator<String> values) {
            this.parameters = updateMap(this.parameters, key, values);
        }

        public void addGroup(@NotNull String key, @NotNull ClonableEnumerator<ParameterSource> groups) {
            this.groups = updateMap(this.groups, key, groups);
        }

        @NotNull
        private static <K, V> Map<K, V> updateMap(@Nullable Map<K, V> m, @NotNull K key, @NotNull V value) {
            Map<K, V> result = m != null ? m : new HashMap<>();
            result.put(key, value);
            return result;
        }
    }

    @NotNull
    private String prepareStringImpl(@NotNull ParameterSource paramSource) {

        Fragment result = this.root.visit(new TemplateNodeVisitor<>() {

            @Nullable
            @Override
            public Fragment visitSequence(@NotNull TemplateNode.Sequence sequence, @NotNull ParameterSource arg) {
                ParameterSource localState;
                if (sequence.name != null) {
                    ClonableEnumerator<ParameterSource> groupSource = arg.findGroup(sequence.name);
                    if (groupSource != null && groupSource.nextValue()) {
                        localState = groupSource.currentValue();
                    } else {
                        return null;
                    }
                } else {
                    localState = arg;
                }

                List<Fragment> ff = new ArrayList<>(sequence.children.size());
                for (TemplateNode node : sequence.children) {
                    Fragment f = node.visit(this, localState);
                    if (f != null) {
                        ff.add(f);
                    } else {
                        return null;
                    }
                }
                return Fragment.ofSequence(ff);
            }

            @Nullable
            @Override
            public Fragment visitText(@NotNull TemplateNode.Text text, @NotNull ParameterSource arg) {
                return Fragment.ofText(text.text);
            }

            @NotNull
            @Override
            public Fragment visitOptional(@NotNull TemplateNode.Optional optional, @NotNull ParameterSource arg) {
                if (optional.hasParameters) {
                    ParameterSource branchState = arg.clone();
                    Fragment result = optional.child.visit(this, branchState);
                    if (result != null) {
                        arg.updateFrom(branchState);
                        return result;
                    } else {
                        return Fragment.empty();
                    }
                } else {
                    return Fragment.empty();
                }
            }

            @Nullable
            @Override
            public Fragment visitParameter(@NotNull TemplateNode.Parameter parameter, @NotNull ParameterSource arg) {
                ClonableEnumerator<String> values = arg.findParameter(parameter.name);
                if (values != null && values.nextValue()) {
                    return Fragment.ofText(values.currentValue());
                } else {
                    return null;
                }
            }

            @Nullable
            @Override
            public Fragment visitAlternatives(@NotNull TemplateNode.Alternatives alternatives, @NotNull ParameterSource arg) {
                for (TemplateNode branch : alternatives.branches) {
                    ParameterSource branchState = arg.clone();
                    Fragment result = branch.visit(this, branchState);
                    if (result != null) {
                        arg.updateFrom(branchState); // if branch successfully applied, then propagate its state
                        return result;
                    }
                }
                return null;
            }

            @NotNull
            @Override
            public Fragment visitRepeat(@NotNull TemplateNode.Repeat repeat, @NotNull ParameterSource arg) {
                List<Fragment> ff = new ArrayList<>();
                ParameterSource branchState = arg.clone();
                Fragment f = repeat.child.visit(this, branchState);
                while (f != null) {
                    arg.updateFrom(branchState);
                    ff.add(f);
                    branchState = arg.clone();
                    f = repeat.child.visit(this, arg);
                }
                return ff.isEmpty() ? Fragment.empty() : Fragment.ofSequence(ff);
            }
        }, paramSource);

        if (result == null) {
            throw new IllegalStateException("Not enough parameters to fulfill template and prepare string");
        } else {
            return result.collectString();
        }
    }

    private interface ClonableEnumerator<T> {

        @NotNull
        T currentValue();

        boolean nextValue();

        @NotNull
        ClonableEnumerator<T> clone();

        @NotNull
        static <T> ClonableEnumerator<T> ofList(@NotNull List<T> list) {
            return new ClonableListEnumerator<T>(list);
        }

        @NotNull
        static <T> ClonableEnumerator<T> ofValue(@NotNull T value) {
            return new ClonableValueEnumerator<>(value);
        }
    }

    private static class ClonableValueEnumerator<T> implements ClonableEnumerator<T> {
        @NotNull
        private final T value;
        private boolean isValueAvailable;

        public ClonableValueEnumerator(@NotNull T value) {
            this(value, true);
        }

        private ClonableValueEnumerator(@NotNull T value, boolean isValueAvailable) {
            this.value = value;
            this.isValueAvailable = isValueAvailable;
        }

        @NotNull
        @Override
        public T currentValue() {
            return this.value;
        }

        @Override
        public boolean nextValue() {
            if (this.isValueAvailable) {
                this.isValueAvailable = false;
                return true;
            } else {
                return false;
            }
        }

        @NotNull
        @Override
        public ClonableEnumerator<T> clone() {
            return new ClonableValueEnumerator<>(this.value, this.isValueAvailable);
        }
    }

    private static class ClonableListEnumerator<T> implements ClonableEnumerator<T> {
        @NotNull
        private final List<T> list;
        private int index;

        public ClonableListEnumerator(@NotNull List<T> list) {
            this(list, -1);
        }

        private ClonableListEnumerator(@NotNull List<T> list, int index) {
            this.list = list;
            this.index = index;
        }

        @NotNull
        public T currentValue() {
            return this.list.get(this.index);
        }

        public boolean nextValue() {
            int nextIndex = this.index + 1;
            if (nextIndex < this.list.size()) {
                this.index = nextIndex;
                return true;
            } else {
                return false;
            }
        }

        @NotNull
        public ClonableEnumerator<T> clone() {
            return new ClonableListEnumerator<>(this.list, this.index);
        }
    }

    public interface IParameterPatternSupplier {
        @NotNull
        String getParamRegex(@NotNull ParameterInfo param);
    }

    @NotNull
    public static StringTemplate parseTemplate(@NotNull String templateString) throws StringTemplateFormatException {
        return parseTemplate(templateString, null);
    }

    @NotNull
    public static StringTemplate parseTemplate(
        @NotNull String templateString, @Nullable IParameterPatternSupplier paramPatternSupplier
    ) throws StringTemplateFormatException {
        List<TemplateSyntaxNode> syntaxNodes = parseTemplate(new Lexer(templateString), TemplateFragmentKind.DEFAULT);
        TemplateNode root = prepareTemplateNode(null, syntaxNodes);
        return new StringTemplate(templateString, root, paramPatternSupplier);
    }

    @NotNull
    private static TemplateNode prepareTemplateNode(@Nullable String groupName, @NotNull List<TemplateSyntaxNode> syntaxNodes) {
        if (syntaxNodes.size() == 1) {
            return prepareTemplateNode(syntaxNodes.getFirst());
        } else {
            return new TemplateNode.Sequence(groupName, syntaxNodes.stream().map(StringTemplate::prepareTemplateNode).toList());
        }
    }

    @NotNull
    private static TemplateNode prepareTemplateNode(@NotNull TemplateSyntaxNode syntaxNode) {
        return switch (syntaxNode.kind) {
            case ERROR, TEXT -> {
                if (syntaxNode.children != null) {
                    yield prepareTemplateNode(syntaxNode.payload(), syntaxNode.children());
                } else if (syntaxNode.payload() != null) {
                    yield new TemplateNode.Text(syntaxNode.payload());
                } else {
                    yield new TemplateNode.Sequence(null, Collections.emptyList());
                }
            }
            case PARAM -> new TemplateNode.Parameter(Objects.requireNonNull(syntaxNode.payload()));
            case OPTIONAL -> new TemplateNode.Optional(
                collectHasParameters(syntaxNode.children()),
                prepareTemplateNode(null, syntaxNode.children())
            );
            case REPEAT -> new TemplateNode.Repeat(
                prepareTemplateNode(null, syntaxNode.children())
            );
            case ALTERNATIVE -> new TemplateNode.Alternatives(
                syntaxNode.children().stream().map(StringTemplate::prepareTemplateNode).toList()
            );
        };
    }

    private static boolean collectHasParameters(@NotNull List<TemplateSyntaxNode> syntaxNodes) {
        for (TemplateSyntaxNode syntaxNode : syntaxNodes) {
            if (syntaxNode.kind == TemplateSyntaxNodeKind.PARAM
                || (syntaxNode.children != null && collectHasParameters(syntaxNode.children))
            ) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static Set<String> collectParameterNames(@NotNull List<TemplateSyntaxNode> syntaxNodes, @NotNull Set<String> names) {
        for (TemplateSyntaxNode syntaxNode : syntaxNodes) {
            if (syntaxNode.kind == TemplateSyntaxNodeKind.PARAM) {
                names.add(syntaxNode.payload());
            }
            if (syntaxNode.kind() != TemplateSyntaxNodeKind.OPTIONAL && syntaxNode.children() != null
                && !(syntaxNode.kind() == TemplateSyntaxNodeKind.TEXT && syntaxNode.payload() != null)
            ) {
                collectParameterNames(syntaxNode.children(), names);
            }
        }
        return names;
    }

    private record Fragment(
        @Nullable String text,
        @Nullable List<Fragment> fragments
    ) {
        private static final Fragment EMPTY = new Fragment("", null);

        @NotNull
        public static Fragment empty() {
            return EMPTY;
        }

        @NotNull
        public static Fragment ofText(@NotNull String value) {
            return new Fragment(value, null);
        }

        @NotNull
        public static Fragment ofSequence(@NotNull List<Fragment> fragments) {
            return new Fragment(null, fragments);
        }

        public void collectTo(@NotNull StringBuilder sb) {
            if (this.text != null) {
                sb.append(this.text);
            }
            if (this.fragments != null) {
                for (Fragment f : this.fragments) {
                    f.collectTo(sb);
                }
            }
        }

        @NotNull
        public String collectString() {
            StringBuilder string = new StringBuilder();
            this.collectTo(string);
            return string.toString();
        }
    }

    public static class StringTemplateFormatException extends Exception {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull
        private final String templateString;

        private final int position;

        public StringTemplateFormatException(@NotNull String templateString, int position, @NotNull String message) {
            super(message);
            this.templateString = templateString;
            this.position = position;
        }

        @NotNull
        public String getTemplateString() {
            return this.templateString;
        }

        public int getPosition() {
            return this.position;
        }
    }
}
