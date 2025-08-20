/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.tipoftheday;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ShowTipOfTheDayHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ShowTipOfTheDayHandler.class);
    private static final String TIPS_XML_FILE = "tips.xml";

    static void showTipOfTheDay(IWorkbenchWindow window) {
        if (UIUtils.isWindowVisible(window.getShell().getDisplay(), ShowTipOfTheDayDialog.class)) {
            return;
        }
        List<String> tips = loadTips();
        if (!CommonUtils.isEmpty(tips)) {
            showTipOfTheDayDialog(tips, window);
        }
    }

    private static void showTipOfTheDayDialog(List<String> tips, IWorkbenchWindow window) {
        if (tips.isEmpty()) {
            return;
        }
        ShowTipOfTheDayDialog tipDialog = new ShowTipOfTheDayDialog(window.getShell(), tips);
        tipDialog.setDisplayShowOnStartup(true);
        tipDialog.open();
    }

    private static List<String> loadTips() {
        List<String> result = new ArrayList<>();
        try (InputStream tipsInputStream = ShowTipOfTheDayHandler.class.getResourceAsStream(TIPS_XML_FILE)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            SAXParser saxParser = factory.newSAXParser();

            TipsXmlHandler handler = new TipsXmlHandler();
            saxParser.parse(tipsInputStream, handler);
            result.addAll(handler.getTips());

            if (!result.isEmpty() && result.size() > 1) {
                Collections.shuffle(result);
            }
        } catch (Throwable e) {
            log.error("Error reading tips", e);
        }
        return result;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        showTipOfTheDay(HandlerUtil.getActiveWorkbenchWindow(event));
        return null;
    }

}
