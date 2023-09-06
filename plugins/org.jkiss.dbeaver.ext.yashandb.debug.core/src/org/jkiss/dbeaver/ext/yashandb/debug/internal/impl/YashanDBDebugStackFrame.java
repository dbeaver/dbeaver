
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGStackFrame;

public class YashanDBDebugStackFrame implements DBGStackFrame {

    private  int level;
    private  String name;
    private  long oid;
    private  int lineNo;
    private  String args;

    public YashanDBDebugStackFrame() {
    }

    public YashanDBDebugStackFrame(int level, String name, int oid, int lineNo, String args) {
        super();
        this.level = level;
        this.name = name;
        this.oid = oid;
        this.lineNo = lineNo;
        this.args = args;
    }

    @Override
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public int getLineNumber() {
        return this.lineNo;
    }

    @Override
    public Object getSourceIdentifier() {
        return this.oid;
    }

    @Override
    public String toString() {
        return "YashanDBDebugStackFrame{" +
                "level=" + level +
                ", name='" + name + '\'' +
                ", oid=" + oid +
                ", lineNo=" + lineNo +
                ", args='" + args + '\'' +
                '}';
    }
}
