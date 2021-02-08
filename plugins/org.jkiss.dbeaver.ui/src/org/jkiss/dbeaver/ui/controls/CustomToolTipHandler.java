/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

/**
 * Shows tooltip for custom controls
 */
public class CustomToolTipHandler {
    private static final Log log = Log.getLog(CustomToolTipHandler.class);

    private final Control control;
    private ToolTipHandler toolTipHandler;
    private volatile String prevToolTip;
    private int toolTipDelay = 500;

    public CustomToolTipHandler(Control control) {
        this.control = control;
    }

    public int getToolTipDelay() {
        return toolTipDelay;
    }

    public void setToolTipDelay(int toolTipDelay) {
        this.toolTipDelay = toolTipDelay;
    }

    /**
     * Sets the tooltip for the whole Grid to the given text.  This method is made available
     * for subclasses to override, when a subclass wants to display a different than the standard
     * SWT/OS tooltip.  Generally, those subclasses would override this event and use this tooltip
     * text in their own tooltip or just override this method to prevent the SWT/OS tooltip from
     * displaying.
     *
     * @param text  tooltip text
     */
    public void updateToolTipText(@Nullable String text)
    {
        if (text != null) {
            // Escape ampersands (#7101)
            text = text.replace("&", "&&");
        }
        ToolTipHandler curHandler = this.toolTipHandler;
        if (!CommonUtils.equalObjects(prevToolTip, text)) {
            // New tooltip
            if (curHandler != null) {
                curHandler.cancel();
            }
            prevToolTip = text;
            control.setToolTipText("");
            this.toolTipHandler = new ToolTipHandler();
            this.toolTipHandler.toolTip = text;
            this.toolTipHandler.schedule(toolTipDelay);
        }
    }


    private class ToolTipHandler extends UIJob {
        private String toolTip;
        ToolTipHandler() {
            super("ToolTip handler");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (!monitor.isCanceled() && !control.isDisposed()) {
                control.setToolTipText(toolTip);
            }
            toolTipHandler = null;
            return Status.OK_STATUS;
        }
    }


}