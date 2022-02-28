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

import org.jkiss.dbeaver.parser.grammar.GrammarInfo;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

public class Parser {
    private final GrammarInfo grammar;
    private final ParserFsm fsm;

    public Parser(GrammarInfo grammar, ParserFsm fsm) {
        this.grammar = grammar;
        this.fsm = fsm;
    }

    /**
     * Parse text
     * @param text
     * @return result of parsing represented with discovered valid sequences of terminals
     */
    public ParseResult parse(String text) {
        Deque<ParserState> queue = new ArrayDeque<>();
        for (ParserFsmNode initialState : fsm.getInitialStates()) {
            queue.addLast(ParserState.initial(initialState));
        }

        ArrayList<ParserDispatchResult> dispatchResults = new ArrayList<>();

        // runs parser finite state machine by dispatching over series of text positions representing terminals being matched
        // and evaluating parsing context until the final state is reached at the end of the text
        List<ParserState> results = new ArrayList<>();
        while (!queue.isEmpty()) {
            ParserState state = queue.removeFirst();

            dispatchResults.clear();
            state.getFsmState().dispatch(text, state.getPosition(), dispatchResults);

            for (ParserDispatchResult result : dispatchResults) {
                ParserStack newStack = evaluateOperations(state.getStack(), result.getStep().getOperations());
                //System.out.println("\tevaluating " + state.getFsmState() + " --> " + result.getStep().getTo());
                if (newStack != null) {
                    //System.out.println("\t\taccepted");
                    ParserState nextState = state.capture(result.getEnd(), result.getStep().getTo(), result.getStep(), newStack);
                    if (result.getStep().getTo() == null || result.getStep().getTo().isEnd()) {
                        if (nextState.getPosition() >= text.length()) {
                            results.add(nextState);
                        }
                    } else {
                        queue.add(nextState);
                    }
                }// else {
                    //System.out.println("\t\tdropped");
                //}
            }
        }

        return new ParseResult(text, grammar, results);
    }

    /**
     * Evaluate grammar graph operations
     * @param stack parsing context
     * @param ops operations to evaluate in the given context
     * @return an updated parsing context stack state
     */
    private static ParserStack evaluateOperations(ParserStack stack, List<GrammarNfaOperation> ops) {
        ParserStack newStack = stack;

        for (GrammarNfaOperation op : ops) {
            // System.out.println("\t\t" + op);
            switch (op.getKind()) {
                case RULE_START:
                case CALL:
                case LOOP_ENTER:
                case SEQ_ENTER:
                    newStack = newStack.push(op.getExprId(), 0, op.getRule());
                    break;
                case RULE_END:
                case RESUME:
                    if (newStack.getExprId() == op.getExprId() && newStack.getRule() == op.getRule()) {
                        newStack = newStack.pop();
                    } else {
                        return null;
                    }
                    break;
                case LOOP_INCREMENT:
                    if (newStack.getExprId() == op.getExprId() && newStack.getExprPosition() <= op.getMaxIterations()) {
                        newStack = newStack.pop().push(op.getExprId(), newStack.getExprPosition() + 1, op.getRule());
                    } else {
                        return null;
                    }
                    break;
                case LOOP_EXIT:
                    if (newStack.getExprId() == op.getExprId() && newStack.getExprPosition() <= op.getMaxIterations()
                        && newStack.getExprPosition() >= op.getMinIterations()) {
                        newStack = newStack.pop();
                    } else {
                        return null;
                    }
                    break;
                case SEQ_STEP:
                    if (newStack.getExprId() == op.getExprId() && newStack.getExprPosition() <= op.getMaxIterations()
                        && newStack.getExprPosition() + 1 == op.getExprPosition()) {
                        newStack = newStack.pop().push(op.getExprId(), op.getExprPosition(), op.getRule());
                    } else {
                        return null;
                    }
                    break;
                case SEQ_EXIT:
                    if (newStack.getExprId() == op.getExprId() && newStack.getExprPosition() + 1 == op.getMaxIterations()) {
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
}
