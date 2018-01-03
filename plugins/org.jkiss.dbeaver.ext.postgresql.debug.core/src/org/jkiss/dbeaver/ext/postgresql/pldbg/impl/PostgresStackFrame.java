package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import org.jkiss.dbeaver.ext.postgresql.pldbg.StackFrame;

public class PostgresStackFrame implements StackFrame {
	
	private final int level;
	private final String name;
	private final String oid;
	private final int lineNo;
	private final String args;
	
	public PostgresStackFrame(int level, String name, String oid, int lineNo, String args) {
		super();
		this.level = level;
		this.name = name;
		this.oid = oid;
		this.lineNo = lineNo;
		this.args = args;
	}

    

	public int getLevel() {
		return level;
	}



	public String getOid() {
		return oid;
	}



	public int getLineNo() {
		return lineNo;
	}



	public String getArgs() {
		return args;
	}



	@Override
	public String getName() {
		return name;
	}



	@Override
	public String toString() {
		return "PostgresStackFrame [level=" + level + ", name=" + name + ", oid=" + oid + ", lineNo=" + lineNo
				+ ", args=" + args + "]";
	}

	
	
}
