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

import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.List;

public class EmptyNodesList implements NodeList, SubnodesList {
    
    public static EmptyNodesList INSTANCE = new EmptyNodesList();
    
    private EmptyNodesList() {
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public List<CustomXPathModelNodeBase> getCollection() {
        return Collections.emptyList();
    }

    @Override
    public CustomXPathModelNodeBase item(int index) {
        return null;
    }

    @Override
    public CustomXPathModelNodeBase getFirst() {
        return null;
    }

    @Override
    public CustomXPathModelNodeBase getLast() {
        return null;
    }
    
}
