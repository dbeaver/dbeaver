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
package org.jkiss.dbeaver.parser.common;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.parser.common.grammar.GrammarInfo;
import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaBuilder.NfaFragment;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaOperation;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaState;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaTransition;
import org.jkiss.dbeaver.parser.common.grammar.nfa.ParseOperationKind;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Parser {
    private final GrammarInfo grammar;
    private final NfaFragment nfa;

    public Parser(GrammarInfo grammar, NfaFragment nfa) {
        this.grammar = grammar;
        this.nfa = nfa;
    }

    private static ImmList<ParsingStep> performPush(
        ImmList<ParsingStep> allPrevSteps,
        GrammarNfaTransition transition,
        int exprPosition
    ) {
        return ImmList.of(new ParsingStep(allPrevSteps, transition, StackFrame.push(
            transition.getOperation().getExprId(),
            exprPosition,
            transition.getOperation().getRule(),
            allPrevSteps.map(s -> s.stack)
        )));
    }

    private static ImmList<ParsingStep> performPop(
        ImmList<ParsingStep> allPrevSteps,
        GrammarNfaTransition transition,
        Predicate<StackFrame> condition
    ) {
        ImmList<ParsingStep> prevSteps = allPrevSteps.filter(
            s -> s.stack.exprId == transition.getOperation().getExprId() && condition.test(s.stack)
        );
        return prevSteps.flatMap(prevStep -> prevStep.stack.pop().map(stack -> new ParsingStep(
            ImmList.of(prevStep), transition, stack
        )));
    }

    private static ImmList<ParsingStep> performPopAndPush(
        ImmList<ParsingStep> allPrevSteps,
        GrammarNfaTransition transition,
        Predicate<StackFrame> condition,
        IntFunction<Integer> exprPosition
    ) {
        ImmList<ParsingStep> prevSteps = allPrevSteps.filter(
            s -> s.stack.exprId == transition.getOperation().getExprId() && condition.test(s.stack)
        );

        int prevStepsCount = prevSteps.count();
        if (prevStepsCount == 1
            || (prevStepsCount > 1 && prevSteps.all(s -> s.stack.exprPosition == prevSteps.peek().stack.exprPosition))
        ) {
            return ImmList.of(new ParsingStep(prevSteps, transition, StackFrame.push(
                transition.getOperation().getExprId(),
                exprPosition.apply(prevSteps.peek().stack.exprPosition),
                transition.getOperation().getRule(),
                prevSteps.flatMap(prevStep -> prevStep.stack.pop())
            )));
        } else {
            return prevSteps.map(prevStep -> new ParsingStep(ImmList.of(prevStep), transition, StackFrame.push(
                transition.getOperation().getExprId(),
                exprPosition.apply(prevStep.stack.exprPosition),
                transition.getOperation().getRule(),
                prevStep.stack.pop()
            )));
        }
    }

    private static ImmList<ParsingStep> evaluateOperation(ImmList<ParsingStep> prevSteps, GrammarNfaTransition transition) {
        GrammarNfaOperation op = transition.getOperation();
        ImmList<ParsingStep> result;
        switch (op.getKind()) {
            case CALL:
            case RESUME:
            case NONE:
                result = prevSteps.map(s -> new ParsingStep(ImmList.of(s), transition, s.stack));
                break;
            case RULE_START:
            case LOOP_ENTER:
            case SEQ_ENTER:
                result = performPush(prevSteps, transition, 0);
                break;
            case RULE_END:
                result = performPop(prevSteps, transition, f -> f.getRule() == op.getRule());
                break;
            case LOOP_INCREMENT:
                result = performPopAndPush(prevSteps, transition, f -> f.getExprPosition() <= op.getMaxIterations(), p -> p + 1);
                break;
            case LOOP_EXIT:
                result = performPop(
                    prevSteps,
                    transition,
                    f -> f.getExprPosition() <= op.getMaxIterations() && f.getExprPosition() >= op.getMinIterations()
                );
                break;
            case SEQ_STEP:
                result = performPopAndPush(
                    prevSteps,
                    transition,
                    f -> f.getExprPosition() <= op.getMaxIterations() && f.getExprPosition() + 1 == op.getExprPosition(),
                    p -> op.getExprPosition()
                );
                break;
            case SEQ_EXIT:
                result = performPop(prevSteps, transition, f -> f.getExprPosition() + 1 == op.getMaxIterations());
                break;
            case TERM:
            default:
                throw new UnsupportedOperationException("Unexpected parse operation kind " + op.getKind());
        }
        return result;
    }

    /**
     * Parse text
     *
     * @return result of parsing represented with discovered valid sequences of terminals
     */
    public ParseResult parse(String text) {
        return parse(text, false, () -> false);
    }

    public ParseResult parse(String text, boolean firstResult, BooleanSupplier cancellationChecker) {
        PositionsQueue queue = new PositionsQueue(text.length(), nfa.getFrom());

        ArrayList<ParserState> results = new ArrayList<>();
        ArrayDeque<LocalState> localStates = new ArrayDeque<>();
        while (queue.isNotEmpty()) { // stepping through positions as terms are being discovered
            for (ParserState state : queue.dequeue()) { // for each local context
                if (state.nfaState == nfa.getTo()) {
                    results.add(state);
                }
                GrammarNfaState.DispatchResult dispatchResult = state.nfaState.dispatch(text, state.position);
                if (dispatchResult != null) {
                    localStates.clear();
                    for (GrammarNfaTransition t : dispatchResult.transitions) {
                        localStates.offer(new LocalState(state.paths, t));
                    }
                    while (!localStates.isEmpty()) { // advance the context as far as possible till next term
                        LocalState localState = localStates.remove();
                        if (localState.transitionToGo.getOperation().getKind() == ParseOperationKind.TERM) {
                            // got to the term, advancing the position and enqueuing for another further dispatch iteration
                            queue.enqueue(state.makeNext(
                                dispatchResult.end,
                                localState.transitionToGo.getTo(),
                                localState.prevSteps.map(s -> new ParsingStep(ImmList.of(s), localState.transitionToGo, s.stack))
                            ));
                        } else {
                            // just apply the context-local operations between the terms
                            ImmList<ParsingStep> stepsDone = evaluateOperation(localState.prevSteps, localState.transitionToGo);
                            if (!stepsDone.isEmpty()) {
                                for (GrammarNfaTransition t : localState.transitionToGo.getTo().getNextByTerm(dispatchResult.term)) {
                                    if (t.getTo() == nfa.getTo()) {
                                        ImmList<ParsingStep> finalSteps = stepsDone.filter(s -> s.stack.isRoot());
                                        if (!finalSteps.isEmpty()) {
                                            results.add(state.makeNext(dispatchResult.end, t.getTo(), finalSteps.map(
                                                s -> new ParsingStep(ImmList.of(s), t, s.stack)
                                            )));
                                        } else {
                                            localStates.offer(new LocalState(stepsDone, t));
                                        }
                                    } else {
                                        localStates.offer(new LocalState(stepsDone, t));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ParseResultImpl(results, text, queue.boundaryPosition, queue.getBoundaryStates());
    }

    private static class ImmList<T> implements Iterable<T> {

        private static final ImmList<?> SENTINEL = new ImmList<>(null, null);

        private final T data;
        private final ImmList<T> next;

        private ImmList(T data, ImmList<T> next) {
            this.data = data;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        public static <T> ImmList<T> empty() {
            return (ImmList<T>) SENTINEL;
        }

        public static <T> ImmList<T> of(T data) {
            return new ImmList<>(data, empty());
        }

        public ImmList<T> push(T data) {
            return new ImmList<>(data, this);
        }

        public ImmList<T> merge(ImmList<T> other) {
            ImmList<T> result = this;
            for (ImmList<T> item = other; item != SENTINEL; item = item.next) {
                result = result.push(item.data);
            }
            return result;
        }

        public int count() {
            int result = 0;
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                result++;
            }
            return result;
        }

        public <R> R aggregate(R initial, BiFunction<R, T, R> action) {
            R result = initial;
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                result = action.apply(result, item.data);
            }
            return result;
        }

        public <R> ImmList<R> map(Function<T, R> action) {
            ImmList<R> result = ImmList.empty();
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                result = result.push(action.apply(item.data));
            }
            return result;
        }

        public ImmList<T> filter(Predicate<T> action) {
            ImmList<T> result = ImmList.empty();
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                if (action.test(item.data)) {
                    result = result.push(item.data);
                }
            }
            return result;
        }

        public boolean all(Predicate<T> action) {
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                if (!action.test(item.data)) {
                    return false;
                }
            }
            return true;
        }

        public boolean any(Predicate<T> action) {
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                if (action.test(item.data)) {
                    return true;
                }
            }
            return false;
        }

        public <R> ImmList<R> flatMap(Function<T, ImmList<R>> action) {
            ImmList<R> result = ImmList.empty();
            for (ImmList<T> item = this; item != SENTINEL; item = item.next) {
                result = result.merge(action.apply(item.data));
            }
            return result;
        }

        public boolean isEmpty() {
            return this == SENTINEL;
        }

        public ImmList<T> pop() {
            if (this.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                return this.next;
            }
        }

        public T peek() {
            if (this.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                return this.data;
            }
        }

        @NotNull
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private ImmList<T> expected = ImmList.this;

                @Override
                public boolean hasNext() {
                    return expected != SENTINEL;
                }

                @Override
                public T next() {
                    if (expected == SENTINEL) {
                        throw new NoSuchElementException();
                    } else {
                        T result = expected.data;
                        expected = expected.next;
                        return result;
                    }
                }
            };
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ImmList[");
            if (this != SENTINEL) {
                sb.append(data == null ? "<NULL>" : data.toString());
                int count = 0;
                for (ImmList<T> item = next; item != SENTINEL; item = item.next) {
                    count++;
                    if (count > 20) {
                        sb.append(", ...");
                        break;
                    }
                    sb.append(", ").append(item.data == null ? "<NULL>" : item.data.toString());
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private static class StackFrame {
        private final int exprId;
        private final int exprPosition;
        private final GrammarRule rule;
        private final ImmList<StackFrame> prev;

        private StackFrame(int exprId, int exprPosition, GrammarRule rule, ImmList<StackFrame> prev) {
            this.exprId = exprId;
            this.exprPosition = exprPosition;
            this.rule = rule;
            this.prev = prev;
        }

        public static StackFrame initial() {
            return new StackFrame(Integer.MIN_VALUE, Integer.MIN_VALUE, null, ImmList.empty());
        }

        public static StackFrame push(int exprId, int exprPosition, GrammarRule rule, ImmList<StackFrame> prev) {
            return new StackFrame(exprId, exprPosition, rule, prev);
        }

        public int getExprId() {
            return exprId;
        }

        public Integer getExprPosition() {
            return exprPosition;
        }

        public GrammarRule getRule() {
            return rule;
        }

        public boolean isRoot() {
            return prev.isEmpty() && exprId == Integer.MIN_VALUE && exprPosition == Integer.MIN_VALUE && rule == null;
        }

        public ImmList<StackFrame> pop() {
            return prev;
        }

        private void formatTo(StringBuilder sb) {
            sb.append("[").append(rule == null ? "<NULL>" : rule.getName()).append(":").append(exprId).append("@").append(exprPosition);
            if (!prev.isEmpty()) {
                sb.append(" ");

                if (sb.length() > 100) {
                    sb.append(", ...]");
                    return;
                }

                prev.forEach(f -> f.formatTo(sb));
            }
            sb.append("]");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("StackFrame");
            this.formatTo(sb);
            return sb.toString();
        }
    }

    private static class ParsingStep {
        public final ImmList<ParsingStep> prev;
        public final GrammarNfaTransition transition;
        public StackFrame stack;

        public ParsingStep(ImmList<ParsingStep> prev, GrammarNfaTransition transition, StackFrame stack) {
            this.prev = prev;
            this.transition = transition;
            this.stack = stack;
        }

        private void formatTo(StringBuilder sb) {
            sb.append("[");
            sb.append(transition);

            ImmList<ParsingStep> steps = prev;
            while (!steps.isEmpty() && steps.pop().isEmpty()) {
                if (sb.length() > 100) {
                    sb.append(", ...]");
                    return;
                }

                sb.append(", ").append(steps.peek().transition);
                steps = steps.peek().prev;
            }

            if (sb.length() > 100) {
                sb.append(", ...]");
                return;
            }

            if (!steps.isEmpty()) {
                steps.forEach(f -> f.formatTo(sb));
            }

            sb.append("]");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ParsingStep");
            this.formatTo(sb);
            return sb.toString();
        }
    }

    private static class ParserState {
        public final ImmList<ParserState> prev;
        public final int position;
        public final GrammarNfaState nfaState;
        public final ImmList<ParsingStep> paths;

        private ParserState(ImmList<ParserState> prev, int position, GrammarNfaState nfaState, ImmList<ParsingStep> paths) {
            this.prev = prev;
            this.position = position;
            this.nfaState = nfaState;
            this.paths = paths;

            ImmList<ParsingStep> q = this.paths.flatMap(p -> p.prev);
            while (!q.isEmpty()) {
                ParsingStep s = q.peek();
                q = q.pop();

                if (s.stack != null) {
                    s.stack = null;

                    for (ParsingStep ps : s.prev) {
                        q = q.push(ps);
                    }
                }
            }
        }

        public static ParserState initial(GrammarNfaState initialState) {
            return new ParserState(
                ImmList.empty(),
                0,
                initialState,
                ImmList.of(new ParsingStep(ImmList.empty(), null, StackFrame.initial()))
            );
        }

        public ParserState makeNext(int position, GrammarNfaState nfaState, ImmList<ParsingStep> path) {
            return new ParserState(ImmList.of(this), position, nfaState, path);
        }

        public ParserState merge(ParserState other) {
            if (other == null) {
                return this;
            } else if (other.position == this.position && other.nfaState == this.nfaState) {
                return new ParserState(this.prev.merge(other.prev), position, nfaState, this.paths.merge(other.paths));
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public String toString() {
            return "ParserState[pos: " + position + ", state:" + nfaState + "]";
        }
    }

    private static class PositionsQueue {
        private final PriorityQueue<Integer> queueOfPositions = new PriorityQueue<Integer>();
        private final List<HashMap<GrammarNfaState, ParserState>> localStatesByPos;

        private int currentPosition = 0;
        private int boundaryPosition = 0;

        @SuppressWarnings("unchecked")
        public PositionsQueue(int positions, GrammarNfaState initialState) {
            localStatesByPos = Arrays.asList(new HashMap[positions + 1]);
            HashMap<GrammarNfaState, ParserState> states = new HashMap<>();
            states.put(initialState, ParserState.initial(initialState));
            localStatesByPos.set(0, states);
            queueOfPositions.offer(0);
        }

        public void enqueue(ParserState state) {
            if (state.position <= currentPosition) {
                throw new IllegalStateException("");
            }
            HashMap<GrammarNfaState, ParserState> states = localStatesByPos.get(state.position);
            if (states == null) {
                states = new HashMap<>();
                states.put(state.nfaState, state);
                localStatesByPos.set(state.position, states);
                queueOfPositions.offer(state.position);
            } else {
                states.compute(state.nfaState, (k, oldState) -> state.merge(oldState));
            }

            if (state.position > boundaryPosition) {
                boundaryPosition = state.position;
            }
        }

        public boolean isNotEmpty() {
            return queueOfPositions.size() > 0;
        }

        public Iterable<ParserState> dequeue() {
            Integer position = queueOfPositions.poll();
            if (position == null) {
                return Collections.emptyList();
            } else {
                currentPosition = position;
                HashMap<GrammarNfaState, ParserState> states = localStatesByPos.get(position);
                if (states == null) {
                    return Collections.emptyList();
                } else {
                    return states.values();
                }
            }
        }

        public Collection<ParserState> getBoundaryStates() {
            return Collections.unmodifiableCollection(localStatesByPos.get(boundaryPosition).values());
        }
    }

    private static class LocalState {
        public final ImmList<ParsingStep> prevSteps;
        public final GrammarNfaTransition transitionToGo;

        public LocalState(ImmList<ParsingStep> prevSteps, GrammarNfaTransition transitionToGo) {
            this.prevSteps = prevSteps;
            this.transitionToGo = transitionToGo;
        }
    }

    private static class PathStep {
        public final PathStep next;
        public final ParserState state;
        public final ParsingStep step;
        public final StackFrame stack;

        private PathStep(PathStep next, ParserState state, ParsingStep step, StackFrame stack) {
            this.next = next;
            this.state = state;
            this.step = step;
            this.stack = stack;
        }

        public static PathStep initial(ParserState state) {
            return new PathStep(null, state, null, StackFrame.initial());
        }

        public PathStep enterState(ParserState state, StackFrame stack) {
            return new PathStep(this, state, null, stack);
        }

        public PathStep enterStep(ParsingStep step, StackFrame stack) {
            return new PathStep(this, state, step, stack);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PathStep[");
            sb.append(this.step.transition);
            for (PathStep step = this.next; step != null; step = step.next) {
                sb.append(", ");
                sb.append(step.step == null ? "<NULL>" : step.step.transition);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private class ParseResultImpl implements ParseResult {
        private final ArrayList<ParserState> results;
        private final String text;
        private final int boundaryPosition;
        private final Collection<ParserState> boundaryStates;

        public ParseResultImpl(ArrayList<ParserState> results, String text, int boundaryPosition, Collection<ParserState> boundaryStates) {
            this.results = results;
            this.text = text;
            this.boundaryPosition = boundaryPosition;
            this.boundaryStates = boundaryStates;
        }

        public boolean isSuccess() {
            return results.size() > 0;
        }

        public int getBoundaryPosition() {
            return boundaryPosition;
        }

        public String[] getBoundaryExpectedContinuations() {
            ArrayDeque<LocalState> localStates = new ArrayDeque<>();
            HashSet<TermPatternInfo> expectedTerms = new HashSet<TermPatternInfo>();
            for (ParserState state : boundaryStates) {
                for (Map.Entry<TermPatternInfo, List<GrammarNfaTransition>> nextByTerm : state.nfaState.getAllNextByTerms().entrySet()) {
                    TermPatternInfo term = nextByTerm.getKey();
                    for (GrammarNfaTransition t : nextByTerm.getValue()) {
                        localStates.offer(new LocalState(state.paths, t));
                    }
                    while (!localStates.isEmpty()) { // advance the context as far as possible till next term
                        LocalState localState = localStates.remove();
                        if (localState.transitionToGo.getOperation().getKind() == ParseOperationKind.TERM) {
                            expectedTerms.add(localState.transitionToGo.getOperation().getPattern());
                        } else {
                            // just apply the context-local operations between the terms
                            ImmList<ParsingStep> stepsDone = evaluateOperation(localState.prevSteps, localState.transitionToGo);
                            if (!stepsDone.isEmpty()) {
                                for (GrammarNfaTransition t : localState.transitionToGo.getTo().getNextByTerm(term)) {
                                    if (t.getTo() == nfa.getTo()) {
                                        ImmList<ParsingStep> finalSteps = stepsDone.filter(s -> s.stack.isRoot());
                                        if (!finalSteps.isEmpty()) {
                                            expectedTerms.add(TermPatternInfo.EOF);
                                        } else {
                                            localStates.offer(new LocalState(stepsDone, t));
                                        }
                                    } else {
                                        localStates.offer(new LocalState(stepsDone, t));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (expectedTerms.size() > 0) {
                return expectedTerms.stream().map(t -> t.pattern).distinct().toArray(String[]::new);
            } else {
                return boundaryStates.stream().flatMap(s -> s.nfaState.getExpectedTerms().stream())
                    .map(t -> t.pattern).distinct().toArray(String[]::new);
            }
        }

        public List<ParseTreeNode> getTrees(boolean withWhitespaces) {
            ImmList<ParseTreeNode> trees = ImmList.empty();
            for (ParserState state : results) {
                trees = trees.merge(findTreePath(state).map(path -> reconstructTree(path, withWhitespaces)));
            }
            return StreamSupport.stream(trees.spliterator(), false).collect(Collectors.toUnmodifiableList());
        }

        /**
         * Collect and evaluate all grammar graph operations of presented parse results
         * producing a set of cached parse trees
         */
        private ImmList<PathStep> findTreePath(ParserState lastState) {
            ImmList<PathStep> queue = ImmList.of(PathStep.initial(lastState));
            ImmList<PathStep> results = ImmList.empty();
            while (!queue.isEmpty()) {
                PathStep pathStep = queue.peek();
                queue = queue.pop();

                if (pathStep.step == null) {
                    for (ParsingStep step : pathStep.state.paths) {
                        queue = applyStep(queue, pathStep, step);
                    }
                } else {
                    for (ParsingStep step : pathStep.step.prev) {
                        if (step.prev.isEmpty()) {
                            // head of the path found
                            // if stack is empty, then we can reconstruct a tree
                            results = results.push(pathStep.enterStep(step, pathStep.stack));
                            return results;
                        } else {
                            ImmList<ParserState> prevStates = pathStep.state.prev.filter(ps -> ps.paths.any(s -> s == step));
                            if (prevStates.isEmpty()) {
                                queue = applyStep(queue, pathStep, step);
                            } else {
                                for (ParserState prevState : prevStates) {
                                    // TODO validate stack consistency along the part
                                    queue = queue.push(pathStep.enterState(prevState, pathStep.stack));
                                }
                            }
                        }
                    }
                }
            }

            if (results.count() > 1) {
                throw new IllegalStateException("too many trees per path");
            }

            return results;
        }

        private ImmList<PathStep> applyStep(ImmList<PathStep> queue, PathStep pathStep, ParsingStep step) {
            GrammarNfaOperation op = step.transition.getOperation();
            StackFrame f = pathStep.stack;
            switch (op.getKind()) {
                case CALL:
                case RESUME:
                case NONE:
                case TERM:
                    queue = queue.push(pathStep.enterStep(step, f));
                    break;
                case RULE_END:
                    queue = queue.push(pathStep.enterStep(step, StackFrame.push(op.getExprId(), 0, op.getRule(), ImmList.of(f))));
                    break;
                case RULE_START:
                    if (f.getExprId() == op.getExprId() && f.getRule() == op.getRule()) {
                        queue = queue.push(pathStep.enterStep(step, f.pop().peek()));
                    }
                    break;
                case SEQ_EXIT:
                    queue = queue.push(
                        pathStep.enterStep(step, StackFrame.push(op.getExprId(), op.getExprPosition(), op.getRule(), ImmList.of(f)))
                    );
                    break;
                case SEQ_STEP:
                    if (f.getExprId() == op.getExprId() && f.getExprPosition() > 0 && f.getExprPosition() - 1 == op.getExprPosition()) {
                        queue = queue.push(
                            pathStep.enterStep(step, StackFrame.push(op.getExprId(), f.getExprPosition() - 1, op.getRule(), f.pop()))
                        );
                    }
                    break;
                case SEQ_ENTER:
                    if (f.getExprId() == op.getExprId() && f.getExprPosition() - 1 == 0) {
                        queue = queue.push(pathStep.enterStep(step, f.pop().peek()));
                    }
                    break;
                case LOOP_EXIT:
                    queue = queue.push(pathStep.enterStep(step, StackFrame.push(op.getExprId(), 0, op.getRule(), ImmList.of(f))));
                    break;
                case LOOP_INCREMENT:
                    if (f.getExprId() == op.getExprId() && f.getExprPosition() <= op.getMaxIterations()) {
                        queue = queue.push(
                            pathStep.enterStep(step, StackFrame.push(op.getExprId(), f.getExprPosition() + 1, op.getRule(), f.pop()))
                        );
                    }
                    break;
                case LOOP_ENTER:
                    if (f.getExprId() == op.getExprId()
                        && f.getExprPosition() <= op.getMaxIterations()
                        && f.getExprPosition() >= op.getMinIterations()
                    ) {
                        queue = queue.push(pathStep.enterStep(step, f.pop().peek()));
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            return queue;
        }

        /**
         * Build parse tree based on a given sequence of parsing steps by evaluating grammar graph operations
         */
        private ParseTreeNode reconstructTree(PathStep treePath, boolean withWhitespaces) {
            GrammarRule skipRule = grammar.getSkipRuleName() == null ? null : grammar.getRule(grammar.getSkipRuleName());
            ParseTreeNode treeRoot = new ParseTreeNode(null, null, 0, text.length(), null, new ArrayList<>());
            ParseTreeNode current = treeRoot;
            int pos = 0;
            int skipDepth = 0;
            ParserState state = treePath.state.prev.peek();
            for (PathStep pathStep = treePath; pathStep != null; pathStep = pathStep.next) {
                ParsingStep step = pathStep.step;
                if (step != null && step.transition != null) {
                    String currentTag = null;
                    GrammarNfaOperation op = step.transition.getOperation();
                    switch (op.getKind()) {
                        case CALL:
                            currentTag = op.getTag();
                            break;
                        case RULE_START:
                            if (skipDepth == 0) {
                                if (!withWhitespaces && op.getRule() == skipRule && skipRule != null) {
                                    skipDepth++;
                                } else {
                                    ParseTreeNode newNode = new ParseTreeNode(
                                        op.getRule(),
                                        currentTag,
                                        pos,
                                        -1,
                                        current,
                                        new ArrayList<>()
                                    );
                                    current.getChildren().add(newNode);
                                    current = newNode;
                                }
                            } else {
                                skipDepth++;
                            }
                            currentTag = null;
                            break;
                        case RULE_END:
                            if (skipDepth == 0) {
                                int endPos = pos;
                                if (current.getChildren().size() > 0) {
                                    endPos = current.getChildren().get(current.getChildren().size() - 1).getEndPosition();
                                }
                                current.setEndPosition(endPos);
                                current = current.getParent();
                            } else {
                                skipDepth--;
                            }
                            break;
                        case TERM:
                            if (skipDepth == 0) {
                                current.getChildren().add(new ParseTreeNode(
                                    null, null /*state.getStep().getTag() */, pos, pathStep.state.position, current, new ArrayList<>()
                                ));
                            }
                            pos = pathStep.state.position;
                            break;
                        default:
                            // do nothing
                            break;
                    }
                }
            }
            return treeRoot;
        }
    }
}
