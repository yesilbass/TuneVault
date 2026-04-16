# Contributing to TuneVault

Thank you for your interest in this project.

## License and permissions

TuneVault is **proprietary software**. The [LICENSE](LICENSE) reserves all rights: there is **no open-source license** granting reuse, redistribution, or the creation of derivative works without **prior written permission** from the copyright holder.

That means:

- **Do not** assume you may copy this codebase into another product, publish a fork as your own app, or use it commercially without explicit authorization.
- **Pull requests and patches** are welcome only when the copyright holder has **already agreed** in writing to your contribution (for example as a collaborator, contractor, or under a separate agreement). Unsolicited PRs may be **closed without review**.

If you want to discuss licensing or collaboration, use a channel the maintainer publishes (for example, contact details on their GitHub profile).

---

## If you are authorized to contribute

The following keeps changes consistent with the rest of the codebase.

### Before you start

- Read **[CODEMAP.md](CODEMAP.md)** for layout and entry points.
- Build with **Java** and **Maven** as described in the main [README](README.md) (`./mvnw` / `mvn`).
- Run the app against a **local MySQL** instance when testing persistence; use environment variables for DB credentials when possible (`TUNEVAULT_DB_URL`, `TUNEVAULT_DB_USER`, `TUNEVAULT_DB_PASSWORD`).

### Code style

- Match existing **package structure** and naming (`com.example.tunevaultfx.*`).
- Prefer **FXML + controller** separation: UI structure in `.fxml`, behavior in `*Controller.java`.
- Keep **controllers** thin; push reusable logic into **services** or **DAOs** as the project already does.
- Use **existing utilities** (`SceneUtil`, `AlertUtil`, `ToastUtil`, etc.) instead of duplicating patterns.
- Follow the project’s **import** and formatting conventions; avoid drive-by reformatting of unrelated files.

### Commits and branches

- Use **focused commits** with clear messages (what changed and why).
- Branch from the default branch using a short descriptive name (e.g. `fix/playlist-reorder`, `feature/search-history`).

### What to include with a change

- **Tests** when you add or fix non-trivial logic (JUnit is already in the project).
- **README or CODEMAP updates** only when your change affects setup, architecture, or user-visible behavior worth documenting.

### Pull request description

Briefly state:

- **Problem** or goal  
- **Approach**  
- **How you tested** (manual steps or automated tests)  
- Any **follow-ups** or known limitations  

---

## Bug reports and feedback (read-only)

If you are **not** contributing code but noticed a problem while **viewing** the repository in line with GitHub’s normal use and the LICENSE, you may **open a GitHub Issue** for a factual bug report or typo in documentation—unless the maintainer prefers another channel.

Please:

- Describe **what happened** vs **what you expected**  
- Include **steps to reproduce** and your **environment** (OS, Java version, relevant config)  
- Do **not** include secrets (passwords, API keys, production URLs)

The maintainer **does not** promise a response timeline; this is a personal/educational project unless stated otherwise.

---

## Questions

General “how does JavaFX work?” questions are better suited to **documentation and forums**. For **TuneVault-specific** questions, see [CODEMAP.md](CODEMAP.md) and the main [README](README.md) first.
