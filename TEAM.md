# Team Information

## Team name
- team-36

## Team members and GitHub Usernames
1. Killu Kaareste -> killu-k


# Git Branch Management

This project follows a simple  workflow to ensure the `main` branch is stable.

---

## Main Branch Rules

- Direct pushes to `main` are not allowed
- All changes must go through a Merge Request (MR)
- At least 1 approval from another team member is required before merging

---

## Branch Types

### Feature Branches

Used for new features and improvements.

Naming format:
```
feature/<week-nr>-<ex-nr>-<team-member-name>
```

Example:
```
feature/week-2-part-3-Killu
```

---

## Development Flow

### Step 1 — Create a Branch

Always branch from the latest `main`:

```bash
git checkout main
git pull
git checkout -b feature/branch-name
```

---

### Step 2 — Work and Commit

Make small, clear commits:

```bash
git add .
git commit -m "Add login validation for password"
```

---

### Step 3 — Push Your Branch

```bash
git push origin feature/branch-name
```

---

### Step 4 — Open a Merge Request

Create a Merge Request:

```
feature/branch-name → main
```

Your MR should include:
- Description of what was changed
- 
---

## Merge Requirements

Before merging into `main`:

- At least 1 team approval
- No unresolved review comments
- Branch is up to date with `main`
- Delete feature branch after merge

---

## What Not To Do

- Do not push directly to `main`
- Do not merge your own MR without review

---

Simple rule:  
Branch → Work → Merge Request → Code Review → Merge

