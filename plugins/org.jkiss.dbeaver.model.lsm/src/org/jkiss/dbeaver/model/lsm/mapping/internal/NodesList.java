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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class NodesList<T extends Node> extends ArrayList<T> implements NodeList {
    public NodesList() {
        super();
    }
    
    public NodesList(int capacity) {
        super(capacity);
    }

    @Override
    public T item(int index) {
        return index < 0 || index >= this.size() ? null : this.get(index);
    }

    @Override
    public int getLength() {
        return this.size();
    }

    public T getFirst() {
        return this.size() > 0 ? this.get(0) : null;
    }

    public T getLast() {
        return this.size() > 0 ? this.get(this.size() - 1) : null;
    }
}
