# Community issue follow-up

The reply workflow handles new comments on open issues labeled `wait for response`.
Any external human reply during the current wait moves the issue to `wait for review`.
The daily workflow also picks up missed replies. Staff comments and unrelated edits
do not restart the 30 days; removing and reapplying the label or reopening does.

## Setup

1. Install a dedicated GitHub App on `dbeaver/dbeaver` with organization **Members: read**.
   No webhook or issue-write permission is needed for the App. Issue writes use the
   workflow's repository token.
2. Set the Actions variable `TRIAGE_APP_CLIENT_ID` and the secret `TRIAGE_APP_PRIVATE_KEY`.
   Set `TRIAGE_PRIVATE_MEMBER` as an Actions secret to the login of a known **private**
   organization member. The workflows check this before interpreting a missing user
   as a nonmember. Update the secret if that member leaves or makes membership public.
3. Run **Check unanswered community issues** from the default branch. By default it
   repairs labels and reports eligible closures in the job summaries without closing.
   After reviewing the candidates, set the Actions variable `TRIAGE_CLOSE_ENABLED`
   to `true` to enable closure on the next sweep. Unset it to return to reporting.

The scheduled check runs daily at 03:17 UTC. If a run flags activity overlapping
a closure, review the issue timeline and reopen if appropriate.
