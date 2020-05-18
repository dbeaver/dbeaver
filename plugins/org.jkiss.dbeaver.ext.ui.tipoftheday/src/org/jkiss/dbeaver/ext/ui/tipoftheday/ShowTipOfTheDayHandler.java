/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowTipOfTheDayHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ShowTipOfTheDayHandler.class);

    public static final String UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP = "ui.show.tip.of.the.day.on.startup";

    static void showTipOfTheDay(IWorkbenchWindow window) {
        List<String> tips = loadTips();
        if (!CommonUtils.isEmpty(tips)) {
            showTipOfTheDayDialog(tips, window);
        }
    }

    private static void showTipOfTheDayDialog(List<String> tips, IWorkbenchWindow window) {
        final ShowTipOfTheDayDialog tipDialog = new ShowTipOfTheDayDialog(window.getShell());
        tipDialog.setDisplayShowOnStartup(true);
        tipDialog.setShowOnStartup(
            CommonUtils.getBoolean(
                DBWorkbench.getPlatform().getPreferenceStore().getString(UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP), true));

        for (String tip : tips) {
            tipDialog.addTip(tip);
        }

        tipDialog.open();

        DBWorkbench.getPlatform().getPreferenceStore().
            setValue(UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP, String.valueOf(tipDialog.isShowOnStartup()));
    }

    private static List<String> loadTips() {
        List<String> result = new ArrayList<>();

        String pathToTipsFile = Platform.getProduct().getProperty("tipsFile");
        if (pathToTipsFile == null) {
            return result;
        }

        URL url;
        try {
            url = FileLocator.find(new URL(pathToTipsFile));
        } catch (MalformedURLException e) {
            log.debug(e);
            return null;
        }
        if (url != null) {
            try (InputStream tipsInputStream = url.openConnection().getInputStream()) {

                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();

                TipsXmlHandler handler = new TipsXmlHandler();
                saxParser.parse(tipsInputStream, handler);
                result.addAll(handler.getTips());

            } catch (SAXException | ParserConfigurationException e) {
                log.error("Unable to parse tips file:", e);
            } catch (IOException ioe) {
                log.error("Tips file wasn't found", ioe);
            }
            if (!result.isEmpty() && result.size() > 1) {
                Collections.shuffle(result);
            }
        }
        return result;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        showTipOfTheDay(HandlerUtil.getActiveWorkbenchWindow(event));
        return null;
    }

}
