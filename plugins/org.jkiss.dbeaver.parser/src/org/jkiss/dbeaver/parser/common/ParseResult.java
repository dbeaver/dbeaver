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

import java.util.List;

/**
 * Parse result represented with discovered valid sequences of terminals
 */
public interface ParseResult {
    boolean isSuccess();
    int getBoundaryPosition();
    String[] getBoundaryExpectedContinuations();

    /**
     * Parse tree describing text structure according to the grammar rules.
     * @param withWhitespaces true to include meaningless parts like whitespaces in a tree
     * @return a collection of parse trees. If there is no ambiguity in grammar then only one tree will be returned.
     */
    List<ParseTreeNode> getTrees(boolean withWhitespaces);
}