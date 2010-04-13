/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class NonConflictingRule implements ISchedulingRule
{
	public boolean contains(ISchedulingRule rule)
	{
		return rule == this;
	}

	public boolean isConflicting(ISchedulingRule rule)
	{
		return rule == this;
	}
}