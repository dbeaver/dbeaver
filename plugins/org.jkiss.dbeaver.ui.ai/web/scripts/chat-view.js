function hasActiveStreamingResponse() {
    return currentStreamingMessageId !== null || streamingMessages.size > 0;
}

function ensureBusyIndicator() {
    if (!isBusy || hasActiveStreamingResponse()) {
        statusBar.remove();
        return;
    }
    if (statusBar.parentElement !== chat || chat.lastElementChild !== statusBar) {
        chat.appendChild(statusBar);
    }
}

function appendChatNode(node, scrollToBottom = true) {
    chat.appendChild(node);
    ensureBusyIndicator();
    if (scrollToBottom) {
        chat.scrollTop = chat.scrollHeight;
    }
}

function clearChat() {
    chat.replaceChildren();
    streamingMessages.clear();
    currentStreamingMessageId = null;
    ensureBusyIndicator();
}

function getClassName(role) {
    switch (role) {
        case 'user':
            return 'right';
        case 'attachment':
            return 'attachment';
        case 'function':
        case 'assistant':
            return 'left';
        default:
            return role;
    }
}

function setBusy(args) {
    hideCenterText();

    isBusy = Boolean(args.busy);
    ensureBusyIndicator();
    if (isBusy) {
        chat.scrollTop = chat.scrollHeight;
    }
}

function showCenterText(args) {
    hideCenterText();

    const centerDiv = document.createElement('div');
    centerDiv.id = 'center-text-content';
    centerDiv.innerHTML = `<div class="center-text-content">${args.text}</div>`;

    document.body.appendChild(centerDiv);
}

function hideCenterText() {
    const content = document.getElementById('center-text-content');
    if (content) {
        content.remove();
    }
}

function settingsChanged() {
    settings.showMessageTime = getSetting(settingKeys.showMessageTime);
    settings.showTimeSpent = getSetting(settingKeys.showTimeSpent);
    settings.showTokensSpent = getSetting(settingKeys.showTokensSpent);
    settings.showTotalTokensSpent = getSetting(settingKeys.showTotalTokensSpent);

    updateMetaVisibility();
}

function updateMetaVisibility() {
    // Chat
    const showChatMeta = settings.showTotalTokensSpent;
    chatMeta.style.display = showChatMeta ? '' : 'none';

    // Messages
    const showMessageMeta = settings.showMessageTime || settings.showTimeSpent || settings.showTokensSpent;
    for (const meta of document.getElementsByClassName('message-meta')) {
        meta.style.display = showMessageMeta ? '' : 'none';
        updateMessageMeta(meta);
    }
}

function getElementMeta(element) {
    const meta = {};
    for (const attr of element.attributes) {
        if (attr.name.startsWith('data-meta-')) {
            const key = attr.name.slice('data-meta-'.length);
            meta[key] = attr.value;
        }
    }
    return meta;
}

function setElementMeta(element, meta) {
    for (const [key, value] of Object.entries(meta)) {
        element.setAttribute(`data-meta-${key}`, value);
    }
}

function updateMessageMeta(element) {
    const meta = getElementMeta(element);
    element.innerText = formatMessageMeta(meta);
}

function formatMessageMeta(meta) {
    let parts = [];
    if (settings.showMessageTime && meta.time)
        parts.push(meta.time);
    if (settings.showTimeSpent && meta.duration)
        parts.push(meta.duration);
    if (settings.showTokensSpent && meta.usage)
        parts.push(meta.usage);
    return parts.join(', ');
}

function hideChatMeta() {
    setSetting(settingKeys.showTotalTokensSpent, false);
}

function updateChatMeta(text) {
    chatMetaText.innerText = text;
}
