const streamingMessages = new Map();
let currentStreamingMessageId = null;


function addMessageChunk(args) {
    hideCenterText();

    let messageId = String(args.id);
    let message = document.getElementById(messageId);

    if (message && !message.classList.contains('streaming')) {
        if (!currentStreamingMessageId) {
            currentStreamingMessageId = `streaming-${messageId}`;
        }
        messageId = currentStreamingMessageId;
        message = document.getElementById(messageId);
    } else if (!currentStreamingMessageId) {
        currentStreamingMessageId = messageId;
    }

    ensureBusyIndicator();

    if (!message) {
        message = document.createElement('div');
        message.id = messageId;
        message.className = 'message left streaming';

        const contentHolder = document.createElement('p');
        const body = document.createElement('p');
        body.className = 'streaming-content';
        contentHolder.appendChild(body);
        message.appendChild(contentHolder);

        appendChatNode(message, false);
        streamingMessages.set(messageId, '');
    }

    let currentContent = streamingMessages.get(messageId) || '';
    currentContent += args.chunk;
    streamingMessages.set(messageId, currentContent);

    const isAtBottom = chat.scrollHeight - chat.scrollTop - chat.clientHeight < 50;

    const streamingContent = message.querySelector('.streaming-content');
    if (streamingContent) {
        const sqlContents = message.querySelectorAll('.sql-content');
        const scrollPositions = Array.from(sqlContents).map(el => ({ left: el.scrollLeft, top: el.scrollTop }));

        const markdown = marked.parse(currentContent);
        streamingContent.innerHTML = parseSqlBlocksStreaming(markdown, args.id);

        const newSqlContents = message.querySelectorAll('.sql-content');
        newSqlContents.forEach((el, i) => {
            if (scrollPositions[i]) {
                el.scrollLeft = scrollPositions[i].left;
                el.scrollTop = scrollPositions[i].top;
            }
        });
    }

    if (isAtBottom) {
        ensureBusyIndicator();
        chat.scrollTop = chat.scrollHeight;
    }
}


function finalizeStreamingMessage(messageId, meta) {
    const message = document.getElementById(messageId);
    if (!message || !message.classList.contains('streaming')) {
        return;
    }

    const content = streamingMessages.get(messageId);
    if (content) {
        const contentHolder = message.querySelector('p');
        if (contentHolder) {
            const body = contentHolder.querySelector('.streaming-content') || contentHolder.querySelector('p');
            if (body) {
                const markdown = marked.parse(content);
                body.innerHTML = parseSqlBlocks(markdown, messageId);
                body.className = '';
            }

            if (meta) {
                let metaElement = contentHolder.querySelector('.message-meta');
                if (!metaElement) {
                    metaElement = document.createElement('p');
                    metaElement.className = 'message-meta';
                    contentHolder.appendChild(metaElement);
                }
                metaElement.innerText = formatMessageMeta(meta);
                setElementMeta(metaElement, meta);
                updateMessageMeta(metaElement);
            }
        }

        streamingMessages.delete(messageId);
    }

    message.classList.remove('streaming');
    if (currentStreamingMessageId === messageId) {
        currentStreamingMessageId = null;
    }
    ensureBusyIndicator();
}
