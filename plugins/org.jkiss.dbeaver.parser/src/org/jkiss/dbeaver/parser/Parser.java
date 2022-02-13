package org.jkiss.dbeaver.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

public class Parser {
    
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

    private final ParseFsm fsm;

    public Parser(ParseFsm fsm) {
        this.fsm = fsm;
    }

    public void parse(String text) {
        Deque<State> queue = new ArrayDeque<>();
        for (var s : fsm.getInitialStates()) {
            queue.addLast(State.initial(s));
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

        System.out.println("Results { ");
        for (State r : results) {
            List<State> states = new ArrayList<>();
            for (State s = r; s != null; s = s.prev) {
                states.add(s);
            }
            Collections.reverse(states);
            int pos = 0;
            for (State s : states) {
                if (s.step != null && s.step.getPattern() != null) {
                    System.out.println(
                            "\t\t\"" + text.substring(pos, s.position) + "\" @" + pos + " is " + s.step.getPattern());
                }
                pos = s.position;
            }

            makeParseTree(text, states);
        }
        System.out.println("} ");
    }

    private Stack evaluateOperations(Stack stack, List<GrammarNfaOperation> ops) {
        for (GrammarNfaOperation op : ops) {
            // System.out.println("\t\t" + op);
            switch (op.getKind()) {
            case RULE_START:
            case CALL:
            case LOOP_ENTER:
            case SEQ_ENTER:
                stack = stack.push(op.getExprId(), 0, op.getRuleName());
                break;
            case RULE_END:
            case RESUME:
                if (stack.exprId == op.getExprId() && stack.ruleName.equals(op.getRuleName())) {
                    stack = stack.pop();
                } else {
                    return null;
                }
                break;
            case LOOP_INCREMENT:
                if (stack.exprId == op.getExprId() && stack.exprPosition <= op.getMaxIterations()) {
                    stack = stack.pop().push(op.getExprId(), stack.exprPosition + 1, op.getRuleName());
                } else {
                    return null;
                }
                break;
            case LOOP_EXIT:
                if (stack.exprId == op.getExprId() && stack.exprPosition <= op.getMaxIterations()
                        && stack.exprPosition >= op.getMinIterations()) {
                    stack = stack.pop();
                } else {
                    return null;
                }
                break;
            case SEQ_STEP:
                if (stack.exprId == op.getExprId() && stack.exprPosition <= op.getMaxIterations()
                        && stack.exprPosition + 1 == op.getExprPosition()) {
                    stack = stack.pop().push(op.getExprId(), op.getExprPosition(), op.getRuleName());
                } else {
                    return null;
                }
                break;
            case SEQ_EXIT:
                if (stack.exprId == op.getExprId() && stack.exprPosition + 1 == op.getMaxIterations()) {
                    stack = stack.pop();
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
        return stack;
    }

    private ParseTreeNode makeParseTree(String text, List<State> states) {
        System.out.println("Tree operations:");
        ParseTreeNode treeRoot = new ParseTreeNode("s", null, new ArrayList<ParseTreeNode>());
        ParseTreeNode current = treeRoot;
        int pos = 0;
        for (State state : states) {
            if (state.step != null && state.step.getOperations() != null) {
                for (GrammarNfaOperation op : state.step.getOperations()) {
                    switch (op.getKind()) {
                    case RULE_START:
                        current.getChilds()
                                .add(new ParseTreeNode(op.getRuleName(), current, new ArrayList<ParseTreeNode>()));
                    case RULE_END:
                        current = current.getParent();
                        System.out.println("    " + op);
                    default:
                        break;
                    }
                }
                System.out.println("  capture term \"" + text.substring(pos, state.position) + "\" @" + pos + " is "
                        + state.step.getPattern());
                pos = state.position;
            }
        }
        return treeRoot;
    }

}
