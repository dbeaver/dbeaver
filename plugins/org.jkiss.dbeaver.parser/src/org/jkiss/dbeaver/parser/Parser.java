/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser;

import java.util.*;

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

public class Parser {

    private final ParseFsm fsm;

    private static class State {
        private final State prev;
        private final ParseState fsmState;
        private final int position;
        private final ParseStep step;
        private final Stack stack;

        private State(State prev, int position, ParseState fsmState, ParseStep step, Stack stack) {
            this.prev = prev;
            this.position = position;
            this.fsmState = fsmState;
            this.step = step;
            this.stack = stack;
        }

        public static State initial(ParseState state) {
            return new State(null, 0, state, null, Stack.initial());
        }

        public State capture(int endPos, ParseState nextState, ParseStep step, Stack stack) {
            return new State(this, endPos, nextState, step, stack);
        }
        
    }

    public static class Stack {
        private final Stack prev;

        private final int exprId;
        private final int exprPosition;
        private final String ruleName;

        private Stack(Stack prev, int exprId, int exprPosition, String ruleName) {
            this.prev = prev;
            this.exprId = exprId;
            this.exprPosition = exprPosition;
            this.ruleName = ruleName;
        }

        public int getExprId() {
            return exprId;
        }

        public int getExprPosition() {
            return exprPosition;
        }

        public String getRuleName() {
            return ruleName;
        }

        public static Stack initial() {
            return new Stack(null, -1, 0, null);
        }

        public Stack push(int exprId, int exprPosition, String ruleName) {
            return new Stack(this, exprId, exprPosition, ruleName);
        }

        public Stack pop() {
            return this.prev;
        }
    }

    public Parser(ParseFsm fsm) {
        this.fsm = fsm;
    }

    public List<ParseTreeNode> parse(String text) {
        Deque<State> queue = new ArrayDeque<>();
        for (ParseState initialState : fsm.getInitialStates()) {
            queue.addLast(State.initial(initialState));
        }

        List<State> results = new ArrayList<>();
        while (!queue.isEmpty()) {
            State state = queue.removeFirst();
            for (ParseDispatchResult result : state.fsmState.dispatch(text, state.position)) {
                Stack newStack = this.evaluateOperations(state.stack, result.getStep().getOperations());
                System.out.println("\tevaluating " + state.fsmState + " --> " + result.getStep().getTo());
                if (newStack != null) {
                    System.out.println("\taccepted");
                    State nextState = state.capture(result.getEnd(), result.getStep().getTo(), result.getStep(), newStack);
                    if (result.getStep().getTo() == null || result.getStep().getTo().isEnd()) {
                        if (nextState.position >= text.length()) {
                            results.add(nextState);
                        }
                    } else {
                        queue.add(nextState);
                    }
                } else {
                    System.out.println("\tdropped");
                }
            }
        }

        List<ParseTreeNode> trees = new ArrayList<>();
        System.out.println("Results { ");
        for (State result : results) {
            List<State> states = new ArrayList<>();
            for (State state = result; state != null; state = state.prev) {
                states.add(state);
            }
            Collections.reverse(states);
            int pos = 0;
            for (State state : states) {
                if (state.step != null && state.step.getPattern() != null) {
                    System.out.println(
                            "\t\t\"" + text.substring(pos, state.position) + "\" @" + pos + " is " + state.step.getPattern());
                }
                pos = state.position;
            }

            ParseTreeNode tree = makeParseTree(text, states);
            System.out.println(tree.collectString());
            trees.add(tree);
        }
        System.out.println("} ");
        return trees;
    }

    private Stack evaluateOperations(Stack stack, List<GrammarNfaOperation> ops) {
        Stack newStack = stack;

        for (GrammarNfaOperation op : ops) {
            // System.out.println("\t\t" + op);
            switch (op.getKind()) {
            case RULE_START:
            case CALL:
            case LOOP_ENTER:
            case SEQ_ENTER:
                newStack = newStack.push(op.getExprId(), 0, op.getRuleName());
                break;
            case RULE_END:
            case RESUME:
                if (newStack.exprId == op.getExprId() && newStack.ruleName.equals(op.getRuleName())) {
                    newStack = newStack.pop();
                } else {
                    return null;
                }
                break;
            case LOOP_INCREMENT:
                if (newStack.exprId == op.getExprId() && newStack.exprPosition <= op.getMaxIterations()) {
                    newStack = newStack.pop().push(op.getExprId(), newStack.exprPosition + 1, op.getRuleName());
                } else {
                    return null;
                }
                break;
            case LOOP_EXIT:
                if (newStack.exprId == op.getExprId() && newStack.exprPosition <= op.getMaxIterations()
                        && newStack.exprPosition >= op.getMinIterations()) {
                    newStack = newStack.pop();
                } else {
                    return null;
                }
                break;
            case SEQ_STEP:
                if (newStack.exprId == op.getExprId() && newStack.exprPosition <= op.getMaxIterations()
                        && newStack.exprPosition + 1 == op.getExprPosition()) {
                    newStack = newStack.pop().push(op.getExprId(), op.getExprPosition(), op.getRuleName());
                } else {
                    return null;
                }
                break;
            case SEQ_EXIT:
                if (newStack.exprId == op.getExprId() && newStack.exprPosition + 1 == op.getMaxIterations()) {
                    newStack = newStack.pop();
                } else {
                    return null;
                }
                break;
            case TERM:
            case NONE:
            default:
                throw new UnsupportedOperationException("Unexpected parse opeation kind " + op.getKind());
            }
        }
        return newStack;
    }

    private ParseTreeNode makeParseTree(String text, List<State> states) {
        System.out.println("Tree operations:");
        ParseTreeNode treeRoot = new ParseTreeNode("s", 0, null, new ArrayList<ParseTreeNode>());
        ParseTreeNode current = treeRoot;
        int pos = 0;
        for (State state : states) {
            if (state.step != null && state.step.getOperations() != null) {
                for (GrammarNfaOperation op : state.step.getOperations()) {
                    switch (op.getKind()) {
                    case RULE_START:
                        ParseTreeNode newNode = new ParseTreeNode(op.getRuleName(), pos, current, new ArrayList<ParseTreeNode>());
                        current.childs.add(newNode);
                        current = newNode;
                        System.out.println("    " + op);
                        break;
                    case RULE_END:
                        current = current.parent;
                        System.out.println("    " + op);
                        break;
                    default:
                        break;
                    }
                }
                if (state.step.getPattern() != null) {
                    current.childs.add(new ParseTreeNode("$", pos, current, new ArrayList<ParseTreeNode>()));
                }
                System.out.println("  capture term \"" + text.substring(pos, state.position) + "\" @" + pos + " is " + state.step.getPattern());
                pos = state.position;
            }
        }
        return treeRoot;
    }
    

}
