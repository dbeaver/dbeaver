/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.util;

public class PairInt
{
    private int obj1;
    private int obj2;

    public PairInt(int x, int y) {
        obj1 = x;
        obj2 = y;
    }

    public PairInt(PairInt p) {
        obj1 = p.first();
        obj2 = p.second();
    }

    public int first() {
        return obj1;
    }
    public int second() {
        return obj2;
    }

    public void setFirst(int obj) {
        obj1 = obj;
    }

    public void setSecond(int obj) {
        obj2 = obj;
    }

    public String toString() {
        String s;
        s = "(" + obj1 + "," + obj2  + ")"; 
        return s;
    }

} 
