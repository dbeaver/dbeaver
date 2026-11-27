// Command community-issue checks a waiting GitHub issue for replies or an expired deadline.
package main

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"os"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/google/go-github/v93/github"
)

const (
	waiting = "wait for response"
	review  = "wait for review"
	timeout = 30 * 24 * time.Hour
)

type triage struct {
	owner, repo string
	number      int
	issues      *github.IssuesService
	members     *github.OrganizationsService
	known       map[string]bool
}

func (t *triage) isMember(ctx context.Context, login string) (bool, error) {
	if member, ok := t.known[login]; ok {
		return member, nil
	}
	member, resp, err := t.members.IsMember(ctx, t.owner, login)
	if err != nil || resp == nil || (resp.StatusCode != http.StatusNoContent && resp.StatusCode != http.StatusNotFound) {
		// Do not include the login or the membership request URL in public logs.
		return false, errors.New("could not determine organization membership")
	}
	t.known[login] = member
	return member, nil
}

func (t *triage) timeline(ctx context.Context) ([]*github.Timeline, error) {
	var events []*github.Timeline
	for event, err := range t.issues.ListIssueTimelineIter(ctx, t.owner, t.repo, t.number, &github.ListOptions{PerPage: 100}) {
		if err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, nil
}

func labelChange(e *github.Timeline, label string) bool {
	return (e.GetEvent() == "labeled" || e.GetEvent() == "unlabeled") && e.Label.GetName() == label
}

func humanComment(e *github.Timeline) bool {
	return e.GetEvent() == "commented" && e.PerformedViaGithubApp == nil && e.User.GetType() != "Bot"
}

type decision struct {
	action string
	period int64 // Timeline event that started this wait.
}

func (t *triage) evaluate(ctx context.Context) (decision, error) {
	events, err := t.timeline(ctx)
	if err != nil {
		return decision{}, err
	}
	issue, _, err := t.issues.Get(ctx, t.owner, t.repo, t.number)
	if err != nil {
		return decision{}, err
	}
	hasLabel := func(name string) bool {
		return slices.ContainsFunc(issue.Labels, func(l *github.Label) bool { return l.GetName() == name })
	}
	if issue.IsPullRequest() || issue.GetState() != "open" || !hasLabel(waiting) {
		return decision{action: "skip"}, nil
	}

	// Timeline order distinguishes reapplications and comments within the same second.
	start := len(events) - 1
	for ; start >= 0; start-- {
		if e := events[start]; e.GetEvent() == "reopened" || labelChange(e, waiting) {
			break
		}
	}
	if start < 0 || events[start].GetEvent() == "unlabeled" || events[start].GetID() == 0 || events[start].GetCreatedAt().IsZero() {
		return decision{action: "inspect wait history"}, nil
	}
	period := events[start].GetID()
	unknown := false
	for _, e := range events[start+1:] {
		if !humanComment(e) {
			continue
		}
		login := e.User.GetLogin()
		if e.User.GetType() != "User" || login == "" || login == "ghost" {
			unknown = true
			continue
		}
		member, err := t.isMember(ctx, login)
		if err != nil {
			return decision{}, err
		}
		if !member {
			return decision{"relabel", period}, nil
		}
	}
	if unknown || hasLabel(review) {
		return decision{action: "inspect manually"}, nil
	}
	if time.Since(events[start].CreatedAt.Time) >= timeout {
		return decision{"timeout", period}, nil
	}
	return decision{"waiting", period}, nil
}

func (t *triage) reconcile(ctx context.Context, closeEnabled bool) (string, error) {
	first, err := t.evaluate(ctx)
	if err != nil {
		return "", err
	}
	switch first.action {
	case "timeout":
		if !closeEnabled {
			return "eligible for closure (no change)", nil
		}
	case "relabel":
	default:
		return first.action, nil
	}
	fresh, err := t.evaluate(ctx)
	if err != nil {
		return "", err
	}
	if first != fresh {
		return "issue changed; deferred", nil
	}
	if fresh.action == "relabel" {
		// Add first so the next sweep can repair a partial label transition.
		if _, _, err := t.issues.AddLabelsToIssue(ctx, t.owner, t.repo, t.number, []string{review}); err != nil {
			return "", err
		}
		_, err := t.issues.RemoveLabelForIssue(ctx, t.owner, t.repo, t.number, waiting)
		if err != nil {
			return "", err
		}
		return "moved to wait for review", nil
	}
	_, _, err = t.issues.Update(ctx, t.owner, t.repo, t.number, github.UpdateIssueRequest{
		State: github.Ptr("closed"), StateReason: github.Ptr("not_planned"),
	})
	if err != nil {
		return "", err
	}
	// GitHub has no atomic compare-and-close. Flag concurrent activity for review.
	events, err := t.timeline(ctx)
	if err != nil {
		return "closed; check the timeline manually", err
	}
	start := slices.IndexFunc(events, func(e *github.Timeline) bool {
		return e.GetID() == fresh.period && (e.GetEvent() == "reopened" || labelChange(e, waiting))
	})
	if start < 0 || slices.ContainsFunc(events[start+1:], func(e *github.Timeline) bool {
		return e.GetEvent() == "reopened" || labelChange(e, waiting) || labelChange(e, review) ||
			(humanComment(e) && !t.known[e.User.GetLogin()])
	}) {
		return "activity overlapped closure; check whether to reopen", errors.New("review the issue closure")
	}
	return "closed after 30 days without an external reply", nil
}

func run(ctx context.Context) error {
	owner, repo, ok := strings.Cut(os.Getenv("GITHUB_REPOSITORY"), "/")
	number, err := strconv.Atoi(os.Getenv("ISSUE"))
	if !ok || owner == "" || repo == "" || err != nil || number <= 0 {
		return errors.New("expected GITHUB_REPOSITORY and a positive ISSUE number")
	}
	newClient := func(token string) (*github.Client, error) {
		return github.NewClient(github.WithAuthToken(token), github.WithHTTPClient(&http.Client{
			Timeout: 30 * time.Second,
			// Never follow a membership redirect to the public-members-only endpoint.
			CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse },
		}))
	}
	issueClient, err := newClient(os.Getenv("ISSUE_TOKEN"))
	if err != nil {
		return err
	}
	memberClient, err := newClient(os.Getenv("MEMBERS_TOKEN"))
	if err != nil {
		return err
	}
	t := triage{owner: owner, repo: repo, number: number, issues: issueClient.Issues,
		members: memberClient.Organizations, known: make(map[string]bool)}
	probe := os.Getenv("PRIVATE_MEMBER")
	if probe == "" {
		return errors.New("missing private membership probe")
	}
	_, public, err := issueClient.Organizations.IsPublicMember(ctx, owner, probe)
	if err != nil || public == nil || public.StatusCode != http.StatusNotFound {
		return errors.New("cannot verify private membership probe")
	}
	if member, err := t.isMember(ctx, probe); err != nil || !member {
		return errors.New("cannot verify private organization membership visibility")
	}
	message, err := t.reconcile(ctx, os.Getenv("TIMEOUT_MODE") == "close")
	if message != "" {
		line := fmt.Sprintf("#%d: %s\n", number, message)
		fmt.Print(line)
		f, openErr := os.OpenFile(os.Getenv("GITHUB_STEP_SUMMARY"), os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0600)
		if openErr != nil {
			return errors.Join(err, openErr)
		}
		_, writeErr := f.WriteString(line)
		return errors.Join(err, writeErr, f.Close())
	}
	return err
}

func main() {
	if err := run(context.Background()); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
