function parseAttachmentContent(content) {
    if (Array.isArray(content)) {
        return content.map(item => {
            if (item.path && !item.name) {
                item.name = item.path.split('/').pop();
            }
            return item;
        });
    }

    if (typeof content !== 'string' || !content.trim()) {
        return [];
    }

    const str = content.trim();

    const parsed = JSON.parse(str);
    if (Array.isArray(parsed)) {
        return parsed.map(item => {
            if (item.path && !item.name) {
                item.name = item.path.split('/').pop();
            }
            return item;
        });
    }

    const matches = str.match(/\{[^}]*\}/g);
    if (!matches) {
        return [];
    }

    return matches.map(block => {
        const inner = block.slice(1, -1).trim();
        if (!inner) return {};
        const pairs = inner.split(/,\s*(?=[\w-]+=)/);
        const obj = {};
        for (const pair of pairs) {
            const eq = pair.indexOf('=');
            if (eq === -1) continue;
            const key = pair.slice(0, eq).trim();
            let val = pair.slice(eq + 1).trim();
            if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
                val = val.slice(1, -1);
            }
            if (/^\d+$/.test(val)) {
                obj[key] = Number(val);
            } else if (/^(true|false)$/i.test(val)) {
                obj[key] = val.toLowerCase() === 'true';
            } else {
                obj[key] = val;
            }
        }
        return obj;
    });
}


function createAttachmentCard(args) {
    let files = [];
    let items = '';
    try {
        files = parseAttachmentContent(args.content);

        if (!Array.isArray(files)) {
            return '<div class="attachment-card"><div class="attachment-error">Error: Invalid attachment data</div></div>';
        }

        items = files.map(f => {
            const iconSrc = `file://${f.icon}`;
            const name = f.name || (f.path ? f.path.split('/').pop() : 'Unknown');
            return `
        <div class="attachment-item" onclick="window.openFileInExplorer('${escapeAttr(f.path)}');" style="cursor: pointer;" title="Click to show in file explorer">
            <img src="${iconSrc}" class="attachment-icon" alt="File"/>
            <div class="attachment-name" title="${escapeHtml(f.path)}">${escapeHtml(name)}</div>
        </div>`;
        }).join('');

    } catch (error) {
        return '<div class="attachment-card"><div class="attachment-error">Error creating attachment: ' + error.message + '</div></div>';
    }

    const canCreateConnection = args.canCreateConnection === true;
    const canImport = args.canImport === true;

    const isImportDataWasExecuted = args.isImportDataWasExecuted === true;
    const isCreateConnectionWasExecuted = args.isCreateConnectionWasExecuted === true;

    let actionsHtml = '';
    if (canCreateConnection || canImport) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-actions-response">
                ${getCreateButton(canCreateConnection, args.id)}
                ${getImportButton(canImport, args.id, files.length)}
            </div>
        `;
    } else {
        if (files.length === 1) {
            let listItems = '';
            try {
                const entries = window.getFileTypeHandlerExtensions();

                listItems = entries.map(e => {
                    return `<button class="attachment-btn" title="Choose ${e} type"
                        onclick="window.onSetFileType(${args.id}, '${e}');">${e}</button>`;
                }).join('');
            } catch (e) {
                listItems = '';
            }

            const headerText = 'Choose type:';

            actionsHtml = listItems ? `
                <div id="attachment-actions-${args.id}" class="attachment-actions-response">
                    <div style="font-size: 12px; opacity: 0.8; margin: 4px 0;">${headerText}</div>
                    <div class="handler-list" style="display:flex; gap:8px; flex-wrap: wrap;">
                        ${listItems}
                    </div>
                </div>
            ` : `
                <div class="attachment-actions-response">No handlers available</div>`;
        } else {
            actionsHtml = `<div class="attachment-actions-response">There are no available actions for that file(s)</div>`;
        }
    }

    if (isCreateConnectionWasExecuted) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-status">
                Open as table was completed
            </div>
        `;
    } else if (isImportDataWasExecuted) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-status">
                Import data from file was completed
            </div>
        `;
    }
    const closeIconSrc = `file://${closeIcon}`;
    return `
        <div class="attachment-card">
            <img src="${closeIconSrc}" 
                 class="attachment-close-icon" 
                 alt="Close" 
                 title="${closeTooltip}"
                 onclick="deleteMessage(${args.id});" />
            <div class="attachment-files">
                ${items}
            </div>
            ${actionsHtml}
        </div>
    `;
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeAttr(str) {
    return String(str).replace(/['"\\]/g, m => ({"'": "&#39;", '"': '&quot;', '\\': '\\\\'}[m]));
}

function getCreateButton(canCreate, messageId) {
    if (!canCreate) return '';
    return `<button class="attachment-btn" title="Open as table" onclick="window.attachmentCreateConnection(${messageId}); showActionCompleted(${messageId}, 'Open as table');">Open as table</button>`;
}

function getImportButton(canImport, messageId, filesCount = 1) {
    if (!canImport) return '';

    const fileWord = filesCount > 1 ? 'files' : 'file';
    const title = `Import data from ${fileWord}`;
    const actionText = `Import data from ${fileWord}`;

    return `<button class="attachment-btn" title="${title}" onclick="window.attachmentExportToTable(${messageId}); showActionCompleted(${messageId}, '${actionText}');">Import data from ${fileWord}</button>`;
}

function onSetFileType(messageId, extensionId) {
    if (typeof window.setAttachmentFileType !== 'function') {
        return;
    }
    const result = window.setAttachmentFileType(messageId, extensionId);
    const canCreate = Array.isArray(result) && !!result[0];
    const canImport = Array.isArray(result) && !!result[1];
    renderAttachmentActions(messageId, canCreate, canImport);
}

function renderAttachmentActions(messageId, canCreate, canImport) {
    const container = document.getElementById(`attachment-actions-${messageId}`);
    if (!container) return;
    if (!canCreate && !canImport) {
        container.innerHTML = '<div>No actions available for this type</div>';
        return;
    }

    const messageElement = document.getElementById(messageId);
    const filesCount = messageElement ? messageElement.querySelectorAll('.attachment-item').length : 1;

    container.innerHTML = `${getCreateButton(canCreate, messageId)} ${getImportButton(canImport, messageId, filesCount)}`;
}

function showActionCompleted(messageId, actionName) {
    const messageElement = document.getElementById(messageId);
    if (!messageElement) {
        return;
    }

    const attachmentCard = messageElement.querySelector('.attachment-card');
    if (!attachmentCard) {
        return;
    }

    const actionsContainer = attachmentCard.querySelector('.attachment-actions-response');
    if (actionsContainer) {
        actionsContainer.classList.add('hidden');
    }

    const existingStatus = attachmentCard.querySelector('.attachment-status');
    if (existingStatus) {
        existingStatus.remove();
    }

    const statusDiv = document.createElement('div');
    statusDiv.className = 'attachment-status';
    statusDiv.textContent = actionName + ' was completed';

    attachmentCard.appendChild(statusDiv);
}
