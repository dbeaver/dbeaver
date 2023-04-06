package org.jkiss.dbeaver.antlr.model.internal;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
