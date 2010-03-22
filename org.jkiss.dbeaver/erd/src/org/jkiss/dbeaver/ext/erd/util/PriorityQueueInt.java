// $Id: PriorityQueueInt.java 88 2006-07-31 19:52:18Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.util;

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
