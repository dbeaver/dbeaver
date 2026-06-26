const chat = document.getElementById('chat');
const chatMeta = document.getElementById('chat-meta');
const chatMetaText = document.getElementById('chat-meta-text');

const statusBar = document.createElement('div');
statusBar.className = 'status-bar';
statusBar.innerHTML = '<span class="loader"></span><span class="status-bar-text">Waiting for response...</span>';
let isBusy = false;

let copyIcon, executeIcon, editorIcon, infoIcon, cleanIcon, closeIcon;
let copyTooltip, executeTooltip, editorTooltip, cleanTooltip, closeTooltip;

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

function initChat(args) {
    copyIcon = args.copy.icon;
    executeIcon = args.execute.icon;
    editorIcon = args.editor.icon;
    infoIcon = args.info.icon;
    cleanIcon = args.clean.icon;
    closeIcon = args.close.icon;

    copyTooltip = args.copy.tooltip;
    executeTooltip = args.execute.tooltip;
    editorTooltip = args.editor.tooltip;
    cleanTooltip = args.clean.tooltip;
    closeTooltip = args.close.tooltip;
    settingKeys = args.settings || {};

    settingsChanged();
}
