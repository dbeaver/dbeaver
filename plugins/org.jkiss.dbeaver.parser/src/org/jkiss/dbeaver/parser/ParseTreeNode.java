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

import java.util.List;

public class ParseTreeNode {
    private final String ruleName;
    private final ParseTreeNode parent;
    private final List<ParseTreeNode> childs;

    public ParseTreeNode(String ruleName, ParseTreeNode parent, List<ParseTreeNode> childs) {
        this.ruleName = ruleName;
        this.parent = parent;
        this.childs = childs;
    }

    public String getRuleName() {
        return ruleName;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChilds() {
        return childs;
    }

}
