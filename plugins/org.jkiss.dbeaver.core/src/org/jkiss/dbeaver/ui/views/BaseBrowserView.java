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

package org.jkiss.dbeaver.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;


/**
 * Base Browser view.
 */
public class BaseBrowserView extends ViewPart {

    private static final Log log = Log.getLog(BaseBrowserView.class);

    public static final String MEMENTO_URL = "url"; //$NON-NLS-1$

    private Browser browser;
    private String initialUrl;

    private Action backAction = new Action("Back", DBeaverIcons.getImageDescriptor(UIIcon.ARROW_LEFT)) {
        @Override
        public void run() {
            browser.back();
        }
    };

    private Action forwardAction = new Action("Forward", DBeaverIcons.getImageDescriptor(UIIcon.ARROW_RIGHT)) {
        @Override
        public void run() {
            browser.forward();
        }
    };

    private Action stopAction = new Action("Stop", DBeaverIcons.getImageDescriptor(UIIcon.REJECT)) {
        @Override
        public void run() {
            browser.stop();
        }
    };

    private Action refreshAction = new Action("Refresh", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH)) {
        @Override
        public void run() {
            browser.refresh();
        }
    };

    /**
     * Constructs a new <code>BaseBrowserView</code>.
     */
    public BaseBrowserView() {
        initialUrl = "about:blank";
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site);
        if (memento != null) {
            String u = memento.getString(MEMENTO_URL);
            if (u != null) {
                initialUrl = u;
            }
        }
    }

    @Override
    public void saveState(IMemento memento) {
        memento.putString(MEMENTO_URL, browser.getUrl());
    }

    @Override
    public void createPartControl(Composite parent) {
        browser = createBrowser(parent, getViewSite().getActionBars());
        browser.setUrl(initialUrl);
    }

    @Override
    public void setFocus() {
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }

    private Browser createBrowser(Composite parent, final IActionBars actionBars) {

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        parent.setLayout(gridLayout);

        browser = new Browser(parent, SWT.NONE);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        browser.setLayoutData(data);

        browser.addProgressListener(new ProgressAdapter() {
            IProgressMonitor monitor = actionBars.getStatusLineManager().getProgressMonitor();
            boolean working = false;
            int workedSoFar;
            @Override
            public void changed(ProgressEvent event) {
                if (event.total == 0) return;
                if (!working) {
                    if (event.current == event.total) return;
                    monitor.beginTask("", event.total); //$NON-NLS-1$
                    workedSoFar = 0;
                    working = true;
                }
                monitor.worked(event.current - workedSoFar);
                workedSoFar = event.current;
            }
            @Override
            public void completed(ProgressEvent event) {
                monitor.done();
                working = false;
            }
        });
        browser.addStatusTextListener(new StatusTextListener() {
            IStatusLineManager status = actionBars.getStatusLineManager();
            @Override
            public void changed(StatusTextEvent event) {
                status.setMessage(event.text);
            }
        });
        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changed(LocationEvent event) {
                backAction.setEnabled(browser.isBackEnabled());
                forwardAction.setEnabled(browser.isForwardEnabled());
//                if (event.top)
//                    location.setText(event.location);
            }
        });
        browser.addTitleListener(new TitleListener() {
            @Override
            public void changed(TitleEvent event) {
                setPartName(event.title);
            }
        });
/*
        browser.addOpenWindowListener(new OpenWindowListener() {
            public void open(WindowEvent event) {
                BaseBrowserView.this.openWindow(event);
            }
        });
*/
        // TODO: should handle VisibilityWindowListener.show and .hide events
        browser.addCloseWindowListener(new CloseWindowListener() {
            @Override
            public void close(WindowEvent event) {
                BaseBrowserView.this.close();
            }
        });
/*
        location.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                browser.setUrl(location.getText());
            }
        });
*/

        // Hook the navigation actons as handlers for the retargetable actions
        // defined in BrowserActionBuilder.
        actionBars.setGlobalActionHandler("back", backAction); //$NON-NLS-1$
        actionBars.setGlobalActionHandler("forward", forwardAction); //$NON-NLS-1$
        actionBars.setGlobalActionHandler("stop", stopAction); //$NON-NLS-1$
        actionBars.setGlobalActionHandler("refresh", refreshAction); //$NON-NLS-1$

        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(backAction);
        toolBarManager.add(forwardAction);
        toolBarManager.add(stopAction);
        toolBarManager.add(refreshAction);

        backAction.setEnabled(false);
        forwardAction.setEnabled(false);

        return browser;
    }

    /**
     * Closes this browser view.
     */
    private void close() {
        IWorkbenchPage page = getSite().getPage();
        IWorkbenchWindow window = page.getWorkbenchWindow();
        page.hideView(this);
    }

    public Browser getBrowser()
    {
        return browser;
    }
}
