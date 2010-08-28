/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama;

public class SugiyamaNodeComparator implements java.util.Comparator<SugiyamaNode> {

    public int compare(SugiyamaNode node1, SugiyamaNode node2) {
        return node2.getOrder() - node1.getOrder();
    }
}
