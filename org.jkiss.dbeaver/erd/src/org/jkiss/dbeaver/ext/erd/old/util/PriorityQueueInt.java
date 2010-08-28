/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.util;

/**
 * Java 1.4 does not provide a priority queue data structure :-(
 */
public class PriorityQueueInt {
    
    private int num;
    
    private int delta = 3;
    
    private int[] priorities;
    
    private int[] pq;
    
    private int[] qp;
    
    public PriorityQueueInt(int max, int[] priorities) {
        this.priorities = priorities;
        this.num = 0;
        pq = new int[max + 1];
        qp = new int[max + 1];
        for (int i = 0; i <= max; i++) {
            pq[i] = 0;
            qp[i] = 0;
        }
    }
    
    public boolean less(int i, int j) {
        return priorities[pq[i]] < priorities[pq[j]];
    }
    
    public void exch(int i, int j) {
        int t = pq[i];
        pq[i] = pq[j];
        pq[j] = t;
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }
    
    public void swim(int k) {
        while (k > 1 && less(k, (k + delta - 2) / delta)) {
            exch(k, (k + delta - 2)/ delta); k = (k + delta - 2) / delta;
        }
    }
    
    public void sink(int k, int N) {
        int j;
        while ((j = delta * (k - 1) + 2) <= N) {
            for (int i = j + 1; i < j+delta && i <= N; i++) {
                if (less(i, j)) {
                    j = i;
                }
            }
            if (!(less(j, k))) {
                break;
            }
            exch(k, j);
            k = j;
        }
    }
    
    public boolean isEmpty() {
        return num == 0;
    }
    
    public void insert(int v) {
        pq[++num] = v;
        qp[v] = num;
        swim(num);
    }
    
    public int getMinimum() {
        exch(1, num);
        sink(1, num-1);
        return pq[num--];
    }
    
    public void lower(int k) {
        swim(qp[k]);
    }
}
