function parseSqlBlocks(htmlContent, messageId) {
    if (!htmlContent) return '';

    const sqlBlockRegex = /<pre><code class="language-sql">([\s\S]*?)<\/code><\/pre>/g;

    let i = 0
    return htmlContent.replace(sqlBlockRegex, (match, sqlCodeHtml) => {
        i++;
        const sqlCode = decodeHtmlEntities(sqlCodeHtml.trim());
        return createSqlForm(sqlCode, messageId + '.' + i, messageId, false);
    });
}

function createSqlForm(sqlCode, formId, messageId, isStreaming = false) {
    const lines = sqlCode.trimEnd().split('<br>');
    const linesSplit = sqlCode.trimEnd().split('\n');
    const actualLines = linesSplit.length > lines.length ? linesSplit : lines;

    const rows = actualLines.map((line, i) => {
        const numberCell = `<td class="line-number">${i + 1}</td>`;
        const codeCell = `<td class="code-line">${line || '&nbsp;'}</td>`;
        return `<tr>${numberCell}${codeCell}</tr>`;
    }).join('');

    const disabledAttr = isStreaming ? 'disabled' : '';
    const disabledClass = isStreaming ? 'disabled' : '';

    return `
        <div class="sql-form" id="${formId}">
            <div class="sql-content">
                <table class="code-table">
                    <tbody>${rows}</tbody>
                </table>
                <div class="sql-textarea" readonly style="display: none;">${sqlCode}</div>
            </div>
        </div>
        <div class="sql-actions">
            <button class="sql-button sql-execute-btn ${disabledClass}" onclick="executeInEditor(getSqlCode('${formId}'))" 
                    data-form-id="${formId}" title="${executeTooltip}" ${disabledAttr}>
                <img class="sql-button-img" src="${executeIcon}" alt="Execute"/>
            </button>
            <button class="sql-button sql-edit-btn ${disabledClass}" onclick="openInEditor(getSqlCode('${formId}'), '${messageId}')" 
                    data-form-id="${formId}" title="${editorTooltip}" ${disabledAttr}>
                <img class="sql-button-img" src="${editorIcon}" alt="Edit"/>
            </button>
            <button class="sql-button sql-copy-btn ${disabledClass}" onclick="setClipboardContents(getSqlCode('${formId}'))" 
                    data-form-id="${formId}" title="${copyTooltip}" ${disabledAttr}>
                <img class="sql-button-img" src="${copyIcon}" alt="Copy"/>
            </button>
        </div>
    `;
}


function getSqlCode(formId) {
    const form = document.getElementById(formId);
    if (form) {
        const codeLines = form.querySelectorAll('.code-line');
        const lines = Array.from(codeLines).map(line => {
            return line.textContent.replace(/\u00A0/g, ' ');
        });
        return lines.join('\n');
    }
    return '';
}

function decodeHtmlEntities(html) {
    const txt = document.createElement("textarea");
    txt.innerHTML = html;
    return txt.value;
}

function parseSqlBlocksStreaming(htmlContent, messageId) {
    if (!htmlContent) return '';

    const sqlBlockRegex = /<pre><code class="language-sql">([\s\S]*?)<\/code><\/pre>/g;

    let i = 0;
    return htmlContent.replace(sqlBlockRegex, (match, sqlCodeHtml) => {
        i++;
        const sqlCode = decodeHtmlEntities(sqlCodeHtml.trim());
        return createSqlForm(sqlCode, messageId + '.' + i, messageId, true);
    });
}
