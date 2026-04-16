# Security policy

The maintainer cares about **clear, responsible handling** of security issues in TuneVault. This document explains how to report problems and what to expect.

## Supported scope

- **In scope:** Security weaknesses in **this repository’s** application code, configuration, and documented setup that could realistically affect **confidentiality, integrity, or availability** for users of the app (e.g. authentication flaws, injection issues, unsafe defaults in shipped code paths).
- **Out of scope (examples):**
  - Issues in **third-party** libraries (report to the upstream project; you may notify the maintainer so dependencies can be upgraded).
  - **Physical** or **local** attacks requiring full control of the victim machine beyond what the app is designed to resist.
  - **Spam**, **social engineering**, or **denial of service** via overwhelming unrelated services without a clear defect in TuneVault itself.
  - **Hypothetical** findings without a plausible exploit path or impact.

TuneVault is a **desktop + database-backed** learning project, not a commercial security product. Expectations should match that context.

---

## How to report a vulnerability

**Do not** open a **public** Issue or discussion for an unfixed security problem. That puts other users at risk.

### Preferred: GitHub private reporting

If [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) is enabled on this repository, use **“Report a vulnerability”** in the **Security** tab. That keeps details private between you and the maintainers.

### Alternative

If private reporting is **not** available, contact the maintainer through a **non-public** channel they publish (for example email or instructions on their GitHub profile). Mark the subject clearly, e.g. `[TuneVault Security]`.

---

## What to include

Helpful reports usually contain:

1. **Summary** — what kind of issue (e.g. injection, auth bypass, sensitive data exposure).
2. **Affected component** — class, FXML flow, endpoint, or feature name if known.
3. **Reproduction steps** — minimal steps from a clean or typical dev setup.
4. **Impact** — what an attacker could do, and who is affected.
5. **Environment** — OS, Java version, app version/commit hash, relevant config (redact secrets).
6. **Optional:** suggested fix or patch (not required).

Avoid sending **malware**, **large binary dumps**, or **personal data** of real users.

---

## Our commitments

- **Acknowledgment:** We aim to acknowledge receipt within a **reasonable** time; this is a side project, so delays are possible.  
- **Assessment:** We will triage severity and reproducibility as best we can.  
- **Coordination:** We prefer **coordinated disclosure**—we ask that you **do not** publish exploit details until we have had time to address or document the issue, typically **90 days** unless agreed otherwise.  
- **Fixes:** We will ship fixes or mitigations when practical; some issues may be **documented as known limitations** instead of fully patched.  
- **Credit:** If you want to be credited in release notes or an advisory, say so; we will respect **anonymous** reports if you prefer.

---

## Safe harbor

If you act in **good faith**—no extortion, no data destruction, no disruption of unrelated systems—we will not pursue legal action for research that **respects** this policy and stays within **reasonable** testing (your own installs, local databases, and test accounts).

This does **not** grant permission to break the law or violate the [LICENSE](LICENSE); it is a statement of **intent** only and may not apply in every jurisdiction.

---

## Dependency and secret hygiene

Contributors and users should:

- Keep **MySQL credentials** and **email/API secrets** out of the repo and out of screenshots.  
- Use **environment variables** or local config that is **gitignored** for sensitive values.  
- **Rotate** any credential that was ever committed or shared by mistake.

If you find **accidentally leaked secrets** in git history, report them **privately** using the same process as for vulnerabilities.