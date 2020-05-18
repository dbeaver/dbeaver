/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;

import java.util.Collections;


/**
 * A command completion proposal.
 */
public class SQLCommandCompletionProposal extends SQLCompletionProposal {


    public SQLCommandCompletionProposal(
            SQLCompletionRequest request,
            SQLCommandHandlerDescriptor descriptor)
    {
        super(request,
            descriptor.getId(),
            descriptor.getId(),
            descriptor.getId().length(),
            descriptor.getIcon(),
            DBPKeywordType.OTHER,
            descriptor.getDescription(),
            null,
            Collections.emptyMap());
    }
}
