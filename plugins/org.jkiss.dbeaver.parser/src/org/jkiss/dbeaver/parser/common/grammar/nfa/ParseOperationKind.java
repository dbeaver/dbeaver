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
package org.jkiss.dbeaver.parser.common.grammar.nfa;

/**
 * Kind of the concrete operation on the parser state according to the part of expression
 */
public enum ParseOperationKind {
    RULE_START,
    RULE_END,
    CALL,
    RESUME,
    LOOP_ENTER,
    LOOP_INCREMENT,
    LOOP_EXIT,
    SEQ_ENTER,
    SEQ_STEP,
    SEQ_EXIT,
    TERM,
    NONE
}
