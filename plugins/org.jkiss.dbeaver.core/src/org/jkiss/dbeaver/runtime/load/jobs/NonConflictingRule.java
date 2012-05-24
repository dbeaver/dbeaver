/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class NonConflictingRule implements ISchedulingRule
{
	@Override
    public boolean contains(ISchedulingRule rule)
	{
		return rule == this;
	}

	@Override
    public boolean isConflicting(ISchedulingRule rule)
	{
		return rule == this;
	}
}