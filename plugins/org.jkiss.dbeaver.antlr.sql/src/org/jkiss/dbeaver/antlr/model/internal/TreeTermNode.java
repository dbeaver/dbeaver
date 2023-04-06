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
package org.jkiss.dbeaver.antlr.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

public class TreeTermNode extends TerminalNodeImpl implements CustomXPathModelTextBase {
    
    private int index = -1;
    
    private Map<String, Object> userData;
    
    public TreeTermNode(Token symbol) {
        super(symbol);
    }
    
    public int getIndex() {
        return index;
    }
    
    @Override
    public void fixup(Parser parser, int index) {
        this.index = index;
    }
    
    @Override
    public SubnodesList getSubnodes() {
        return EmptyNodesList.INSTANCE;
    }
    
    @Override
    public NodeList getChildNodes() {
        return EmptyNodesList.INSTANCE;
    }
    
    @Override
    public short getNodeType() {
        return Node.TEXT_NODE;
    }
    
    @Override
    public String getNodeName() {
        return "#text";
    }
    
    @Override
    public Map<String, Object> getUserDataMap(boolean createIfMissing) {
        return userData != null ? userData : (userData = new HashMap<>());
    }    
}
