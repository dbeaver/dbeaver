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

/**
 * State of the parser
 */
class ParserState {
    private final ParserState prev;
    private final long id;
    private final ParserFsmNode fsmState;
    private final int position;
    private final ParserFsmStep step;
    private final ParserStack stack;

    private ParserState(ParserState prev, long id, int position, ParserFsmNode fsmState, ParserFsmStep step, ParserStack stack) {
        this.prev = prev;
        this.id = id;
        this.position = position;
        this.fsmState = fsmState;
        this.step = step;
        this.stack = stack;
    }

    public static ParserState initial(long id, ParserFsmNode state) {
        return new ParserState(null, id, 0, state, null, ParserStack.initial());
    }

    public ParserState getPrev() {
        return prev;
    }

    public long getId() {
        return id;
    }

    public ParserFsmNode getFsmState() {
        return fsmState;
    }

    public int getPosition() {
        return position;
    }

    public ParserFsmStep getStep() {
        return step;
    }

    public ParserStack getStack() {
        return stack;
    }

    public ParserState capture(long id, int endPos, ParserFsmNode nextState, ParserFsmStep step, ParserStack stack) {
        ParserState capturedState = new ParserState(this.prev, this.id, this.position, this.fsmState, this.step, null);
        return new ParserState(capturedState, id, endPos, nextState, step, stack);
    }
}