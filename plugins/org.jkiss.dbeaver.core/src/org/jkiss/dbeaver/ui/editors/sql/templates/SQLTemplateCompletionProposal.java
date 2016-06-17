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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.Log;


/**
 * A template completion proposal.
 * <p>
 * Clients may subclass.</p>
 *
 * @since 3.0
 */
public class SQLTemplateCompletionProposal extends TemplateProposal {

    private static final Log log = Log.getLog(SQLTemplateCompletionProposal.class);

    public SQLTemplateCompletionProposal(Template template, TemplateContext context, IRegion region, Image image) {
        this(template, context, region, image, 0);
    }

    public SQLTemplateCompletionProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
        super(template, context, region, image, relevance);
    }
}
