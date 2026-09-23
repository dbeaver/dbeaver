/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.macros;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.xml.sax.Attributes;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Global SQL editor macros registry.
 * <p>
 * Stores user-defined macros in a workspace configuration file
 * ({@link SQLMacrosConstants#MACROS_CONFIG_FILE}) and reloads it when it changes on disk.
 */
public class SQLMacrosRegistry {

    private static final Log log = Log.getLog(SQLMacrosRegistry.class);

    private static final String ROOT_ELEMENT = "macros"; //$NON-NLS-1$
    private static final String MACRO_ELEMENT = "macro"; //$NON-NLS-1$
    private static final String ATTR_ID = "id"; //$NON-NLS-1$
    private static final String ATTR_NAME = "name"; //$NON-NLS-1$
    private static final String ATTR_INDEX = "shortcut"; //$NON-NLS-1$
    private static final String ATTR_ACTION = "action"; //$NON-NLS-1$
    private static final String ATTR_EXECUTE = "execute"; //$NON-NLS-1$
    private static final String QUERY_ELEMENT = "query"; //$NON-NLS-1$

    private static SQLMacrosRegistry instance;

    private final List<SQLMacro> macros = new ArrayList<>();
    private boolean loaded;

    public static synchronized SQLMacrosRegistry getInstance() {
        if (instance == null) {
            instance = new SQLMacrosRegistry();
        }
        return instance;
    }

    private SQLMacrosRegistry() {
    }

    /**
     * Returns an immutable copy of the configured macros.
     */
    @NotNull
    public synchronized List<SQLMacro> getMacros() {
        ensureLoaded();
        return List.copyOf(macros);
    }

    /**
     * Returns the macro assigned to the specified shortcut slot or null if no macro is assigned to it.
     */
    @Nullable
    public synchronized SQLMacro getMacroByShortcutIndex(int shortcutIndex) {
        ensureLoaded();
        for (SQLMacro macro : macros) {
            if (macro.getShortcutIndex() == shortcutIndex) {
                return macro;
            }
        }
        return null;
    }

    /**
     * Returns the macro assigned to the apply-macro command with the specified id or null if it doesn't match any macro.
     */
    @Nullable
    public synchronized SQLMacro getMacroByCommandId(@NotNull String commandId) {
        if (!commandId.startsWith(SQLMacrosConstants.APPLY_MACRO_COMMAND_PREFIX)) {
            return null;
        }
        String number = commandId.substring(SQLMacrosConstants.APPLY_MACRO_COMMAND_PREFIX.length());
        int index = CommonUtils.toInt(number, -1) - 1;
        if (index < 0 || index >= SQLMacrosConstants.MACRO_KEY_COUNT) {
            return null;
        }
        return getMacroByShortcutIndex(index);
    }

    /**
     * Creates a new macro with a generated id and stores it.
     */
    @NotNull
    public synchronized SQLMacro createMacro(
            @NotNull String name,
            @NotNull String query,
            int shortcutIndex,
            @NotNull MacroAction action
    ) {
        SQLMacro macro = new SQLMacro(UUID.randomUUID().toString(), name, query, shortcutIndex, action);
        updateMacro(macro);
        return macro;
    }

    /**
     * Stores the given macro. If another macro already uses the same shortcut slot, that macro is replaced.
     */
    public synchronized void updateMacro(@NotNull SQLMacro macro) {
        ensureLoaded();
        Iterator<SQLMacro> iterator = macros.iterator();
        while (iterator.hasNext()) {
            SQLMacro existing = iterator.next();
            if (CommonUtils.equalObjects(existing.getId(), macro.getId())
                || existing.getShortcutIndex() == macro.getShortcutIndex()) {
                iterator.remove();
            }
        }
        macros.add(macro);
        save();
    }

    /**
     * Removes the given macro from the configuration.
     */
    public synchronized void deleteMacro(@NotNull SQLMacro macro) {
        ensureLoaded();
        if (macros.remove(macro)) {
            save();
        }
    }

    private void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            load();
            WorkspaceConfigEventManager.addConfigChangedListener(SQLMacrosConstants.MACROS_CONFIG_FILE, o -> load());
        }
    }

    private synchronized void load() {
        macros.clear();
        try {
            String content = DBWorkbench.getPlatform().getProductConfigurationController()
                    .loadConfigurationFile(SQLMacrosConstants.MACROS_CONFIG_FILE);
            if (CommonUtils.isEmpty(content)) {
                return;
            }
            try (StringReader is = new StringReader(content)) {
                SAXReader parser = new SAXReader(is);
                parser.parse(new MacrosParser());
            }
        } catch (Throwable ex) {
            log.warn("Can't load SQL macros configuration from " + SQLMacrosConstants.MACROS_CONFIG_FILE, ex);
        }
    }

    private synchronized void save() {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save SQL macros configuration");
                return;
            }
            StringWriter out = new StringWriter();
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setBeautify(true);
            xml.startElement(ROOT_ELEMENT);
            for (SQLMacro macro : macros) {
                xml.startElement(MACRO_ELEMENT);
                xml.addAttribute(ATTR_ID, macro.getId());
                xml.addAttribute(ATTR_NAME, macro.getName());
                xml.addAttribute(ATTR_INDEX, macro.getShortcutIndex());
                xml.addAttribute(ATTR_ACTION, macro.getAction().getId());
                xml.startElement(QUERY_ELEMENT);
                xml.addText(macro.getQuery());
                xml.endElement();
                xml.endElement();
            }
            xml.endElement();
            xml.flush();
            out.flush();

            DBWorkbench.getPlatform().getProductConfigurationController()
                    .saveConfigurationFile(SQLMacrosConstants.MACROS_CONFIG_FILE, out.getBuffer().toString());
        } catch (Throwable ex) {
            log.warn("Failed to save SQL macros configuration to " + SQLMacrosConstants.MACROS_CONFIG_FILE, ex);
        }
    }

    private class MacrosParser implements SAXListener {

        private String macroId;
        private String macroName;
        private int macroIndex;
        private MacroAction macroAction;
        private String macroQuery;
        private StringBuilder queryBuilder;

        @Override
        public void saxStartElement(@NotNull SAXReader reader, @Nullable String namespaceURI,
                                    @NotNull String localName, @NotNull Attributes attributes) {
            if (MACRO_ELEMENT.equals(localName)) {
                macroId = attributes.getValue(ATTR_ID);
                macroName = attributes.getValue(ATTR_NAME);
                macroIndex = CommonUtils.toInt(attributes.getValue(ATTR_INDEX), 0);
                macroAction = MacroAction.byId(
                        attributes.getValue(ATTR_ACTION),
                        CommonUtils.toBoolean(attributes.getValue(ATTR_EXECUTE), false));
                macroQuery = null;
            } else if (QUERY_ELEMENT.equals(localName)) {
                queryBuilder = new StringBuilder();
            }
        }

        @Override
        public void saxText(@NotNull SAXReader reader, @NotNull String data) {
            if (queryBuilder != null) {
                queryBuilder.append(data);
            }
        }

        @Override
        public void saxEndElement(@NotNull SAXReader reader, @Nullable String namespaceURI, @NotNull String localName) {
            if (QUERY_ELEMENT.equals(localName) && queryBuilder != null) {
                macroQuery = queryBuilder.toString();
                queryBuilder = null;
            } else if (MACRO_ELEMENT.equals(localName)) {
                if (CommonUtils.isNotEmpty(macroId) && CommonUtils.isNotEmpty(macroName)) {
                    macros.add(new SQLMacro(
                            macroId,
                            macroName,
                            CommonUtils.notNull(macroQuery, ""), //$NON-NLS-1$
                            macroIndex,
                            macroAction));
                }
                macroId = null;
                macroName = null;
                macroIndex = 0;
                macroAction = MacroAction.INSERT;
                macroQuery = null;
            }
        }
    }
}