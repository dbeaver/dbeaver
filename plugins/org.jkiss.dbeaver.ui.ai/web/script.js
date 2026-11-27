const chat = document.getElementById('chat');
const chatMeta = document.getElementById('chat-meta');
const chatMetaText = document.getElementById('chat-meta-text');
const aiNotice = document.getElementById('ai-notice');

const statusBar = document.createElement('div');
statusBar.className = 'status-bar';
statusBar.setAttribute('aria-hidden', 'true');
statusBar.innerHTML = '<span class="loader"></span><span class="status-bar-text"></span>';
let isBusy = false;

let copyIcon, executeIcon, editorIcon, infoIcon, cleanIcon, closeIcon;
const copyTooltip = '%#ai_chat_message_copy_to_clipboard_tip';
const executeTooltip = '%#ai_chat_message_execute_query_tip';
const editorTooltip = '%#ai_chat_message_copy_to_editor_tip';
const cleanTooltip = '%#ai_chat_message_clear_up_to_here_tip';
const closeTooltip = '%#ai_chat_message_delete_message_tip';

const settings = {
    showMessageTime: false,
    showTimeSpent: false,
    showTokensSpent: false,
    showTotalTokensSpent: false
};

let settingKeys = {};

document.addEventListener('click', function(event) {
    if (event.target.matches('a.interactive-link')) {
        event.preventDefault();
        const messageId = event.target.getAttribute('data-message-id');
        if (window.executeFunction && messageId) {
            window.executeFunction(parseInt(messageId));
        }
    }
});

document.addEventListener('dragenter', function(event) {
    event.preventDefault();
    event.stopPropagation();
    window.onDragEnter(event.dataTransfer.types);
});


marked.setOptions({
    gfm: true,
    breaks: true,
});

marked.use({
    walkTokens(token) {
        if (token.type === 'code' || token.type === 'codespan') return;

        if (token.type === 'text' && typeof token.text === 'string') {
            token.text = token.text.replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;');
            token.text = token.text.replace(/ {2,}/g, m => '&nbsp;'.repeat(m.length));
        }
    }
});


const PREF_LINK_RE = /^javascript:openPreferencePage\('[a-zA-Z0-9_.]+'\)$/;

DOMPurify.addHook('uponSanitizeAttribute', (node, data) => {
    if (data.attrName === 'href' && PREF_LINK_RE.test(data.attrValue)) {
        data.forceKeepAttr = true;
    }
});

function sanitizeHtml(html) {
    return DOMPurify.sanitize(html);
}

function initChat(args) {
    copyIcon = args.copy.icon;
    executeIcon = args.execute.icon;
    editorIcon = args.editor.icon;
    infoIcon = args.info.icon;
    cleanIcon = args.clean.icon;
    closeIcon = args.close.icon;

    settingKeys = args.settings || {};
    aiNotice.textContent = '%#ai_chat_ai_notice';
    statusBar.querySelector('.status-bar-text').textContent = '%#ai_chat_a11y_waiting_for_response' + '...';

    initA11y(args.a11y || {});
    settingsChanged();
    updateAiNoticeVisibility();
}
function formatChatString(template, ...values) {
    return template.replace(/\{(\d+)\}/g, (match, index) => values[index] ?? match);
}
function setThemeStylesheet({href}) {
    document.getElementById('theme-stylesheet').href = href;
}
