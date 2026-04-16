# Versioning (TuneVaultFX)

**Source of truth:** `pom.xml` → `<version>…</version>`.

Suffix **`SNAPSHOT`** means “moving development line,” not a formal release. This project is a **local desktop demo**; versions mark **milestones**, not individual Git commits.

---

## When to bump (not every commit)

| Bump | From → To (example) | When |
|------|---------------------|------|
| **Patch** | `0.1.0-SNAPSHOT` → `0.1.1-SNAPSHOT` | You cut a **tagged** or clearly named **bugfix** milestone (several fixes, demo stable again). |
| **Minor** | `0.1.z-SNAPSHOT` → `0.2.0-SNAPSHOT` | **User-visible** feature area lands (e.g. local “upload” demo, new major screen, playback behavior change worth calling out in [CHANGELOG](CHANGELOG.md)). |
| **Major** | `0.x.y-SNAPSHOT` → `1.0.0-SNAPSHOT` (then later `1.0.0`) | You declare **“first 1.0 demo”**—portfolio story, not technical necessity. |

**Do not** bump because commit count went up. **Do** bump when you intentionally finish a **chunk of work** you’d describe to someone else or tag on GitHub.

---

## What to update when you bump

1. **`pom.xml`** — set new `<version>` (keep `-SNAPSHOT` until you remove it for a rare “final” tag).
2. **`CHANGELOG.md`** — move **[Unreleased]** items under a new `## [x.y.z-SNAPSHOT] - YYYY-MM-DD` section; leave **[Unreleased]** ready for the next cycle.
3. **`SettingsPageController`** — only if the fallback string should match (it mirrors `pom.xml` when the manifest has no `Implementation-Version`).
4. **Git tag** (optional) — e.g. `v0.2.0` when you drop `-SNAPSHOT` for a showcase build.

---

## For AI-assisted edits in this repo

When making substantive changes, **read this file**. Suggest a **minor** bump when a **new demo feature** ships; **patch** when the milestone is **fixes/stability** only. **Ask the maintainer** before a **major** jump to `1.0.0`. Default between milestones: accumulate under **[Unreleased]** only.
