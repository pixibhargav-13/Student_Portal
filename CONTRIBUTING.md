# Git & Pull Request Conventions

Rules for branches, commits and pull requests (PRs, called merge requests / MRs in GitLab) on
Student Portal. They apply to backend and frontend. Project setup is in
[PROJECT_GUIDE.md](PROJECT_GUIDE.md).

## Contents

1. [Branches](#1-branches)
2. [Branch names](#2-branch-names)
3. [Commit messages](#3-commit-messages)
4. [Pull request titles](#4-pull-request-titles)
5. [Pull request description](#5-pull-request-description)
6. [Daily workflow](#6-daily-workflow)
7. [Review and merge rules](#7-review-and-merge-rules)
8. [Releases and hotfixes](#8-releases-and-hotfixes)
9. [Quick reference](#9-quick-reference)

---

## 1. Branches

| Branch | Purpose | Who merges into it |
|---|---|---|
| `main` | Production. Every commit here is a released version. | Lead only, from `develop` (release) or `hotfix/*` |
| `develop` | Integration branch. Deployed to staging. **All PRs target this branch.** | Anyone, through an approved PR |
| `feature/*`, `fix/*`, ... | One branch per task, created from `develop` | Deleted after merge |

Nobody pushes directly to `main` or `develop`; everything goes through a PR.

## 2. Branch names

```
<type>/<issue-number>-<short-description>
```

- **type**: one of the types below.
- **issue-number**: the GitHub issue number for the task. Leave it out only for tiny chores with no
  issue.
- **short-description**: 2–5 words, lowercase, words joined with `-`.
- Only lowercase letters, digits, `-` and one `/`. No spaces, no capitals, no names of people.

| Type | Use for |
|---|---|
| `feature/` | New functionality |
| `fix/` | Bug fix on `develop` |
| `hotfix/` | Urgent fix on production, branched from `main` |
| `refactor/` | Code change with no behaviour change |
| `chore/` | Build, dependencies, config, CI |
| `docs/` | Documentation only |
| `test/` | Adding or fixing tests only |

**Good**

```
feature/12-tutor-profile-api
feature/15-otp-login-screen
fix/21-otp-resend-timer
hotfix/30-payment-webhook-500
refactor/18-user-service-search
chore/bump-springdoc
docs/api-error-codes
```

**Bad**

```
bhargav-work          # no type, a person's name
Feature/TutorProfile  # capitals, no issue
fix_otp               # underscore, no issue number
feature/12            # no description
new-changes           # says nothing
```

## 3. Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org) (the Parmeshwari ERP history
already follows it):

```
<type>(<scope>): <summary>

<optional body: why the change was needed>

<optional footer: Closes #12>
```

- **type**: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`, `build`, `ci`, `style`.
- **scope** (optional): the module or area touched.
  - Backend: `user-management`, `tutor`, `parent`, `booking`, `payment`, `api-contract`, `common`,
    `web-app`, `db`.
  - Frontend: `auth`, `admin`, `parent`, `tutor`, `ui`, `api`, `routing`.
- **summary**: imperative mood ("add", not "added" / "adds"), lowercase start, no full stop, at most
  72 characters. Say what changes for the user or the code, not "updated files".
- One logical change per commit. Don't mix a feature with unrelated formatting.

**Good**

```
feat(user-management): add mobile OTP login and signup
fix(auth): keep user logged in after page reload
refactor(common): move pagination defaults into PaginationUtils
docs: add project guide and git conventions
feat(tutor)!: rename fee field to monthlyFee

BREAKING CHANGE: frontend must send monthlyFee instead of fee.
```

**Bad**

```
changes
fixed bug
WIP
Updated UserService.java
feat: Added Tutor Profile API and fixed login and changed colors.
```

Breaking API changes (renamed or removed fields, changed paths) get `!` after the type and a
`BREAKING CHANGE:` footer, and must be announced to the frontend at standup.

## 4. Pull request titles

The PR title uses the **same format as a commit message**, because with squash merge the title
becomes the commit on `develop`:

```
<type>(<scope>): <summary>
```

| Branch | PR title |
|---|---|
| `feature/12-tutor-profile-api` | `feat(tutor): add tutor profile create and update APIs` |
| `feature/15-otp-login-screen` | `feat(auth): add OTP login and signup screens` |
| `fix/21-otp-resend-timer` | `fix(auth): restart resend timer after a new OTP is sent` |
| `hotfix/30-payment-webhook-500` | `fix(payment): handle duplicate Razorpay webhook events` |

- Mark unfinished work as a **Draft PR** instead of putting "WIP" in the title.
- Link the issue in the description with `Closes #12` so it closes on merge.

## 5. Pull request description

Use this template (also saved in `.github/pull_request_template.md`, so GitHub fills it in):

```markdown
## What
Short description of the change.

## Why
Closes #<issue>

## How to test
1. Steps a reviewer can follow locally (Swagger calls, screens to open).
2. Expected result.

## API changes
- [ ] No API change
- [ ] OpenAPI spec changed (list endpoints / fields; mark breaking changes)

## Screenshots
Frontend only: before / after, desktop and 375px mobile.

## Checklist
- [ ] Branch is up to date with `develop`
- [ ] Builds locally (`./mvnw.cmd clean install` / `npm run build`)
- [ ] Tests added or updated for new rules
- [ ] No secrets, `.env` or `application-local.yaml` committed
- [ ] DB scripts are re-runnable (`IF NOT EXISTS`)
```

Keep PRs small: aim for under ~400 changed lines (generated code excluded). Split big features into
several PRs, e.g. "tutor profile API" then "tutor availability API".

## 6. Daily workflow

```bash
# 1. Start from the latest develop
git checkout develop
git pull origin develop
git checkout -b feature/12-tutor-profile-api

# 2. Work and commit in small steps
git add -p
git commit -m "feat(tutor): add tutor profile entity and table"

# 3. Before opening the PR, bring in the latest develop
git fetch origin
git rebase origin/develop        # or: git merge origin/develop

# 4. Push and open a PR into develop
git push -u origin feature/12-tutor-profile-api
gh pr create --base develop --fill
```

- Push at least once a day so work isn't lost and the team can see progress.
- If you rebased an already-pushed branch, use `git push --force-with-lease`, never `--force`, and
  only on your own branch.
- Resolve conflicts on your branch, not on `develop`.

## 7. Review and merge rules

- **1 approval required** before merge. Backend PRs: reviewed by the lead. Frontend PRs: reviewed by
  the other frontend developer; the lead also reviews anything that calls a new API.
- The author merges after approval (so the author handles any last conflicts).
- Merge method: **Squash and merge** into `develop`. Delete the branch after merge.
- `develop` → `main` releases use **Create a merge commit** so the release is visible in history.
- Reviewers answer within the same working day. Comments prefixed with `nit:` are optional; anything
  else must be resolved or discussed before merge.
- Don't approve your own PR, and don't merge with failing builds.

Recommended GitHub settings (Settings → Branches → Add rule) for `main` and `develop`: require a pull
request, require 1 approval, block force pushes, and auto-delete head branches after merge.

## 8. Releases and hotfixes

**Release** (end of each sprint, or when staging is signed off):

1. Open a PR `develop` → `main` titled `release: v0.2.0`.
2. After merge, tag it:
   ```bash
   git checkout main && git pull
   git tag -a v0.2.0 -m "Release v0.2.0"
   git push origin v0.2.0
   ```
3. Versions follow `MAJOR.MINOR.PATCH`. Stay on `0.x` until public launch (`v1.0.0`). New features
   raise MINOR, fixes raise PATCH.

**Hotfix** (production bug that can't wait):

1. Branch from `main`: `hotfix/<issue>-<short-description>`.
2. PR into `main`, get it approved, merge, tag a PATCH version (`v0.2.1`).
3. Merge `main` back into `develop` right away so the fix isn't lost.

## 9. Quick reference

| What | Format | Example |
|---|---|---|
| Branch | `<type>/<issue>-<short-description>` | `feature/12-tutor-profile-api` |
| Commit | `<type>(<scope>): <summary>` | `feat(tutor): add tutor profile APIs` |
| PR title | same as commit | `fix(auth): restart resend timer after new OTP` |
| PR target | `develop` (hotfix: `main`) | |
| Merge | squash into `develop`, merge commit into `main` | |
| Tag | `vMAJOR.MINOR.PATCH` | `v0.2.0` |
