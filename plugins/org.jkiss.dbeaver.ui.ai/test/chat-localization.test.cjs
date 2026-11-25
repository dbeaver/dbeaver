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
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

const plugin = path.resolve(__dirname, '..');
const source = path.join(plugin, 'src/org/jkiss/dbeaver/ui/ai/chat');
const renderer = fs.readFileSync(path.join(source, 'controls/WebViewMessageRenderer.java'), 'utf8');
const fields = fs.readFileSync(path.join(source, 'internal/AIChatMessagesUI.java'), 'utf8');
const mappings = [...renderer.matchAll(/Map\.entry\("(\w+)", AIChatMessagesUI\.(\w+)\)/g)];
const locales = ['', 'ar', 'de', 'es', 'fr', 'it', 'ja', 'ko', 'pl', 'pt_BR', 'ro', 'ru', 'tw', 'uk', 'zh'];

function loadMessages(locale) {
    const file = path.join(source, 'internal/AIChatMessagesUI' + (locale ? '_' + locale : '') + '.properties');
    const entries = [...fs.readFileSync(file, 'utf8').matchAll(/^(\w+)\s*=\s*(.*)$/gm)];
    const messages = Object.fromEntries(entries.map(([, key, value]) => [key, value.trim()]));
    for (const [, , key] of mappings) {
        assert.ok(messages[key], `${locale || 'en'}: missing ${key}`);
        assert.equal(entries.filter(([, name]) => name === key).length, 1, `Duplicate ${key}`);
        assert.ok(fields.includes('public static String ' + key + ';'), `Missing NLS field ${key}`);
    }
    return messages;
}

function createChat(strings, waiting) {
    const statusText = {};
    const nodes = {};
    const statusBar = {setAttribute() {}, querySelector: () => statusText};
    const document = {
        getElementById: id => nodes[id] || (nodes[id] = {}),
        createElement: () => statusBar,
        addEventListener() {}
    };
    const context = vm.createContext({
        document,
        window: {getFileTypeHandlerExtensions: () => []},
        marked: {setOptions() {}, use() {}},
        DOMPurify: {addHook() {}},
        initA11y: args => { context.a11y = args; },
        settingsChanged() {},
        updateAiNoticeVisibility() {}
    });
    for (const script of ['script.js', 'scripts/attachments.js']) {
        vm.runInContext(fs.readFileSync(path.join(plugin, 'web', script), 'utf8'), context);
    }
    const action = {icon: '', tooltip: 'Delete "message"'};
    context.initChat({
        copy: action, execute: action, editor: action, info: action, clean: action, close: action,
        strings, a11y: {waitingForResponse: waiting}, notice: ''
    });
    return {context, nodes, statusText, document};
}

const english = loadMessages('');
for (const locale of locales) {
    test(`chat status and attachment messages use ${locale || 'en'} translations`, () => {
        const messages = loadMessages(locale);
        const strings = Object.fromEntries(mappings.map(([, name, key]) => {
            assert.deepEqual(messages[key].match(/\{\d+\}/g), english[key].match(/\{\d+\}/g), key);
            return [name, messages[key]];
        }));
        const waiting = messages.ai_chat_a11y_waiting_for_response;
        const {context, statusText, nodes, document} = createChat(strings, waiting);
        assert.equal(statusText.textContent, waiting + '...');
        assert.equal(context.a11y.waitingForResponse, waiting);

        const attachment = {id: 7, content: [{name: 'data.csv', path: '/data.csv'}]};
        const escape = context.escapeHtml;
        assert.ok(context.createAttachmentCard(attachment).includes(escape(strings.noHandlers)));
        assert.ok(context.createAttachmentCard({...attachment, content: []}).includes(escape(strings.noActions)));
        assert.ok(context.getImportButton(true, 7, 1).includes(escape(strings.importFile)));
        assert.ok(context.getImportButton(true, 7, 2).includes(escape(strings.importFiles)));

        for (const [action, flag, key] of [
            ['open', 'isCreateConnectionWasExecuted', 'openCompleted'],
            ['import', 'isImportDataWasExecuted', 'importCompleted']
        ]) {
            const restored = context.createAttachmentCard({...attachment, [flag]: true});
            assert.ok(restored.includes(escape(strings[key])));
            const card = {querySelector: () => null, appendChild: status => { card.status = status; }};
            nodes[7] = {querySelector: () => card};
            document.createElement = () => ({});
            context.showActionCompleted(7, action);
            assert.equal(card.status.textContent, strings[key]);
        }
    });
}

test('translated labels and error details stay text, including quotes and markup', () => {
    const strings = Object.fromEntries(mappings.map(([, name, key]) => [name, english[key]]));
    strings.openAsTable = 'Ouvrir l\'élément "<table>"';
    strings.importFile = 'Importer l\'élément "<fichier>"';
    strings.attachmentError = 'Erreur d\'attachement : {0}';
    const {context} = createChat(strings, 'Réponse');
    for (const html of [context.getCreateButton(true, 7), context.getImportButton(true, 7)]) {
        assert.ok(html.includes('&#039;'));
        assert.ok(html.includes('&quot;'));
        assert.ok(html.includes('&lt;'));
        for (const [, code] of html.matchAll(/onclick="([^"]*)"/g)) {
            new vm.Script(code);
            assert.ok(!code.includes('élément'));
        }
    }
    const detail = '<img src=x onerror=alert(1)>';
    context.parseAttachmentContent = () => { throw new Error(detail); };
    const html = context.createAttachmentCard({id: 7});
    assert.ok(html.includes('Erreur d&#039;attachement : ' + context.escapeHtml(detail)));
    assert.ok(!html.includes('<img'));
});
