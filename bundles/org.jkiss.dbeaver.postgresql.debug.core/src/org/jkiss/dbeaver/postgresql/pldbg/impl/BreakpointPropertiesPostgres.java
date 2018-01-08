package org.jkiss.dbeaver.postgresql.pldbg.impl;

import org.jkiss.dbeaver.postgresql.pldbg.BreakpointProperties;

public class BreakpointPropertiesPostgres implements BreakpointProperties {
	
	private final long lineNo;
	private final boolean onStart;
	private final long targetId;
	private final boolean all;
	
	
	
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


	public BreakpointPropertiesPostgres(long lineNo, long targetId) {
		this.lineNo = lineNo;
		this.onStart = lineNo < 0;
		this.targetId = targetId;
		this.all = targetId < 0;
	}

	public BreakpointPropertiesPostgres(long lineNo) {
		this.lineNo = lineNo;
		this.onStart = lineNo < 0;
		this.targetId = -1;
		this.all = true;
	}

	public BreakpointPropertiesPostgres() {
		this.lineNo = -1;
		this.onStart = true;
		this.targetId = -1;
		this.all = true;
	}

	
	
}
