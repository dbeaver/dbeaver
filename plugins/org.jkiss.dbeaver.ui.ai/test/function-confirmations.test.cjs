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

const script = fs.readFileSync(path.resolve(__dirname, '../web/scripts/function-confirmations.js'), 'utf8');

function createElement() {
    const element = {
        children: [], dataset: {}, className: '',
        classList: {add: name => { element.className += ' ' + name; }},
        setAttribute() {},
        appendChild: child => { element.children.push(child); }
    };
    return element;
}

function addConfirmation(args) {
    const chat = {};
    const context = vm.createContext({
        document: {createElement, createTextNode: createElement},
        window: {},
        chatStrings: {allowTitlePrefix: 'Allow', allowTitleMiddle: 'to run tool', allowOptions: 'Options'},
        hideCenterText() {},
        makeItemFocusable() {},
        appendChatNode: node => { chat.node = node; },
        announce: text => { chat.announced = text; }
    });
    vm.runInContext(script, context);
    context.addFunctionConfirmation({messageId: 1, functionIndex: 0, title: 'Allow DBeaver to run tool Query', ...args});
    return chat;
}

function collect(node, found = []) {
    found.push(node);
    for (const child of node.children) {
        collect(child, found);
    }
    return found;
}

test('dangerous function shows a warning and full argument previews as plain text', () => {
    const sql = 'SELECT \'<img src=x onerror=alert(1)>\' FROM pg_catalog.pg_class';
    const chat = addConfirmation({
        arguments: {sqlQuery: sql, maxRows: 10},
        warning: 'This action may be unsafe.',
        previews: [{label: 'SQL', value: sql}]
    });
    const nodes = collect(chat.node);
    assert.ok(nodes.some(node => node.className.includes('confirmation-card-dangerous')));
    assert.ok(nodes.some(node => node.className === 'confirmation-warning' && node.textContent.includes('This action may be unsafe.')));
    const card = chat.node.children[0];
    const preview = card.children.find(node => node.className === 'fn-detail-section');
    assert.deepEqual(preview.children.map(node => node.textContent), ['SQL', sql]);
    assert.ok(nodes.every(node => node.innerHTML === undefined));
    assert.equal(chat.announced, 'Allow DBeaver to run tool Query. This action may be unsafe.');
});

test('ordinary function keeps the standard confirmation', () => {
    const chat = addConfirmation({arguments: {table: 'public.orders'}, previews: []});
    const nodes = collect(chat.node);
    assert.ok(nodes.every(node => !node.className.includes('dangerous') && node.className !== 'confirmation-warning'));
    assert.ok(chat.node.children[0].children.every(node => node.className !== 'fn-detail-section'));
    assert.equal(chat.announced, 'Allow DBeaver to run tool Query');
});
