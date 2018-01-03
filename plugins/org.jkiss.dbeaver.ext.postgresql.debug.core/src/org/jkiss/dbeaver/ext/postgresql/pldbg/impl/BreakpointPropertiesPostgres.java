package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import org.jkiss.dbeaver.ext.postgresql.pldbg.BreakpointProperties;

public class BreakpointPropertiesPostgres implements BreakpointProperties {
	
	private final long lineNo;
	private final boolean onStart;
	private final long targetId;
	private final boolean all;
	private final boolean global;
	
	
	public long getLineNo() {
		return lineNo;
	}



	public boolean isOnStart() {
		return onStart;
	}



	public long getTargetId() {
		return targetId;
	}



	public boolean isAll() {
		return all;
	}

	public boolean isGlobal() {
		return global;
	}

	public BreakpointPropertiesPostgres(long lineNo, long targetId,boolean global) {
		this.lineNo = lineNo;
		this.onStart = lineNo < 0;
		this.targetId = targetId;
		this.all = targetId < 0;
		this.global = global;
	}

	public BreakpointPropertiesPostgres(long lineNo,boolean global) {
		this.lineNo = lineNo;
		this.onStart = lineNo < 0;
		this.targetId = -1;
		this.all = true;
		this.global = global;
	}

	public BreakpointPropertiesPostgres(boolean global) {
		this.lineNo = -1;
		this.onStart = true;
		this.targetId = -1;
		this.all = true;
		this.global = global;
	}



	@Override
	public String toString() {
		return "BreakpointPropertiesPostgres [lineNo=" + lineNo + ", onStart=" + onStart + ", targetId=" + targetId
				+ ", all=" + all + ", global=" + global + "]";
	}

	
	
}

