/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.versiongen;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Generates version descriptor from product descriptor
 */
public class GenerateVersionTask extends Task
{

    private String targetDirectory;
    private String productDescriptor;
    private String versionNumber;

    public void execute() throws BuildException
    {
    }

    public void setProductDescriptor(String msg)
    {
        this.productDescriptor = msg;
    }

    public void setTargetDirectory(String targetDirectory)
    {
        this.targetDirectory = targetDirectory;
    }

    public void setVersionNumber(String versionNumber)
    {
        this.versionNumber = versionNumber;
    }
}
