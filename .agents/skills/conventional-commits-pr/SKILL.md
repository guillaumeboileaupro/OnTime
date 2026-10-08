---
name: conventional-commits-pr
description: Apply Conventional Commits 1.0.0 and prepare clear pull requests for Git repositories. Use when creating commits, choosing commit messages, organizing changes into commits, opening a PR, or drafting a PR title and description.
---

# Conventional Commits and Pull Requests

For OnTime, read docs/GIT_POLICY.md first. Its identity, attribution and
authorization rules govern this repository. Preserve GadgetTech contributor
credits; never claim the imported code was originally authored by Guillaume.

Produce an explicit, reviewable Git history and a focused pull request. Follow repository-specific instructions when they are stricter than this skill.

## Before changing Git state

- Inspect `git status`, the current branch, recent commits, and repository instructions.
- Preserve unrelated user changes. Never include them in a commit or PR.
- Do not rewrite shared history, push, open a PR, merge, or delete a branch unless the user has authorized that action.
- If the current branch is the default branch and a PR is expected, create a short descriptive feature branch before committing.

## Organize commits

- Keep each commit logically coherent and independently understandable.
- Split unrelated changes into separate commits when practical.
- Stage only the files and hunks belonging to the commit.
- Run the relevant tests, linters, formatters, or builds before committing. Report checks that could not be run.
- Review the staged diff before creating the commit.

## Write Conventional Commit messages

Use Conventional Commits 1.0.0:

```text
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

Use `feat` for a new feature and `fix` for a bug fix. Common additional types are:

- `docs` for documentation only
- `test` for tests only
- `refactor` for behavior-preserving restructuring
- `perf` for performance improvements
- `build` for build system or dependency changes
- `ci` for CI configuration
- `chore` for maintenance that fits no more precise type
- `revert` for reverting earlier work

Choose a short noun as the optional scope when it adds useful context. Write a concise imperative description without a trailing period. Use the body to explain motivation, tradeoffs, or non-obvious behavior rather than repeating the diff.

Mark a breaking change with `!` before the colon and/or a footer exactly like:

```text
BREAKING CHANGE: explain what changed and how users should migrate
```

Use Git-style trailers for issue references or attribution when relevant, such as `Refs: #123` or `Closes: #123`. Do not invent issue numbers.

## Prepare the pull request

- Confirm the branch contains only the intended commits and compare it with the actual base branch.
- Re-run appropriate checks against the final branch state.
- Use a concise PR title in Conventional Commit form so squash merging can preserve a valid history.
- Write a PR description that includes:
  - what changed and why;
  - the main implementation decisions when they help reviewers;
  - verification performed, with exact commands when useful;
  - screenshots or recordings for visible UI changes when available;
  - linked issues and breaking or migration notes when applicable.
- Do not claim tests passed unless they were executed successfully.
- Before opening the PR, show or summarize the final title, body, base branch, and head branch when the user has not already approved them.
- After opening it, return the PR link and mention any remaining checks, review needs, or known limitations.

## Source

Use the authoritative [Conventional Commits 1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/) when an edge case is unclear.
