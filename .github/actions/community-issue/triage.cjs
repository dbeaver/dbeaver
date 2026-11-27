const waiting = 'wait for response';
const review = 'wait for review';
const timeout = 30 * 24 * 60 * 60 * 1000;

module.exports = async function triage({github, context, core, members}) {
    const issue_number = Number(process.env.ISSUE);
    const issue = {...context.repo, issue_number};
    const org = issue.owner;
    const mode = process.env.TIMEOUT_MODE;
    const privateMember = process.env.PRIVATE_MEMBER;
    const issues = github.rest.issues;

    // Do not follow redirects to the public-members-only endpoint.
    async function membershipStatus(client, path, username) {
        try {
            const result = await client.request(`GET /orgs/{org}/${path}/{username}`, {
                org, username, request: {redirect: 'manual'},
            });
            return result.status;
        } catch (error) {
            if (error.status === 404) return 404;
            // Avoid logging the private membership probe's login or request details.
            throw new Error(`Organization membership lookup failed (${error.status ?? 'network error'})`);
        }
    }

    if (!privateMember || await membershipStatus(github, 'public_members', privateMember) !== 404 ||
        await membershipStatus(members, 'members', privateMember) !== 204) {
        throw new Error('Cannot verify private organization membership visibility');
    }
    const membership = new Map();
    async function isMember(login) {
        if (!membership.has(login)) {
            const status = await membershipStatus(members, 'members', login);
            if (![204, 404].includes(status)) throw new Error(`Unexpected membership status ${status}`);
            membership.set(login, status === 204);
        }
        return membership.get(login);
    }

    const timeline = () => github.paginate(issues.listEventsForTimeline, {...issue, per_page: 100});
    async function evaluate() {
        const events = await timeline();
        const {data} = await issues.get(issue);
        const labels = data.labels.map(label => label.name);
        if (data.pull_request || data.state !== 'open' || !labels.includes(waiting)) return {action: 'skip'};
        // A reopen with the label still present starts a new wait, as does reapplication.
        const start = events.findLastIndex(e => e.event === 'reopened' ||
            (['labeled', 'unlabeled'].includes(e.event) && e.label.name === waiting));
        const entry = events[start];
        const since = Date.parse(entry?.created_at);
        if (!entry?.node_id || !Number.isFinite(since) || entry.event === 'unlabeled') {
            return {action: 'inspect wait history'};
        }
        let unknown = false;
        for (const event of events.slice(start + 1)) {
            if (event.event !== 'commented' || event.performed_via_github_app || event.user?.type === 'Bot') continue;
            if (event.user?.type !== 'User' || !event.user.login || event.user.login === 'ghost') {
                unknown = true;
            } else if (!await isMember(event.user.login)) {
                return {action: 'relabel', period: entry.node_id};
            }
        }
        if (unknown || labels.includes(review)) return {action: 'inspect manually'};
        return {action: Date.now() - since >= timeout ? 'timeout' : 'waiting', period: entry.node_id};
    }

    async function report(message) {
        core.info(`#${issue_number}: ${message}`);
        await core.summary.addRaw(`#${issue_number}: ${message}`).write();
    }

    const first = await evaluate();
    if (first.action !== 'relabel' && !(first.action === 'timeout' && mode === 'close')) {
        return report(first.action === 'timeout' ? 'eligible for closure (no change)' : first.action);
    }
    const fresh = await evaluate();
    if (fresh.action !== first.action || fresh.period !== first.period) return report('issue changed; deferred');
    if (fresh.action === 'relabel') {
        // Add first: the next sweep can finish removing the old label after a partial failure.
        await issues.addLabels({...issue, labels: [review]});
        await issues.removeLabel({...issue, name: waiting});
        return report('moved to wait for review');
    }
    await issues.update({...issue, state: 'closed', state_reason: 'not_planned'});
    // GitHub cannot atomically close an issue conditional on its latest comment.
    const after = await timeline();
    const start = after.findIndex(e => e.node_id === fresh.period);
    if (start < 0 || after.slice(start + 1).some(e => e.event === 'reopened' ||
        (['labeled', 'unlabeled'].includes(e.event) && [waiting, review].includes(e.label.name)) ||
        (e.event === 'commented' && !e.performed_via_github_app && e.user?.type !== 'Bot' &&
            !membership.get(e.user?.login)))) {
        await report('activity overlapped closure; check whether to reopen');
        core.setFailed('Review the community issue closure');
        return;
    }
    await report('closed after 30 days without an external reply');
};
