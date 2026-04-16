# Third-party notices

TuneVault (**this project’s original code**) is proprietary; see [LICENSE](LICENSE).

The application **bundles or depends on** open-source and third-party components. This file is a **good-faith summary** for transparency. **Verify** license text in each artifact (JAR `META-INF/LICENSE`, project homepages, or [Maven Central](https://central.sonatype.com/)) before you rely on it for compliance. The copyright holder does not warrant the accuracy of this list.

---

## Direct runtime dependencies

Versions match `pom.xml` at the time this file was last updated.

| Component | Version | License (common designation) |
|-----------|---------|----------------------------|
| [SLF4J Simple](https://www.slf4j.org/) | 1.7.36 | MIT |
| [HikariCP](https://github.com/brettwooldridge/HikariCP) | 5.1.0 | Apache-2.0 |
| [OpenJFX](https://openjfx.io/) (controls, fxml, web, swing, media) | 21.0.6 | GPL-2.0 with Classpath Exception (see OpenJFX / OpenJDK terms) |
| [ControlsFX](https://github.com/controlsfx/controlsfx) | 11.2.1 | BSD-3-Clause |
| [FormsFX](https://github.com/dlsc-software-consulting-gmbh/formsfx) | 11.6.0 | Apache-2.0 |
| [ValidatorFX](https://github.com/synedra/ValidatorFX) | 0.6.1 | Apache-2.0 |
| [Ikonli JavaFX](https://github.com/kordamp/ikonli) | 12.3.1 | Apache-2.0 |
| [BootstrapFX](https://github.com/kordamp/bootstrapfx) | 0.4.0 | MIT |
| [TilesFX](https://github.com/HanSolo/tilesfx) | 21.0.9 | Apache-2.0 |
| [FXGL](https://github.com/AlmasB/FXGL) | 17.3 | MIT |
| [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/) | 9.6.0 | Oracle license (GPL v2 with FOSS License Exception and related terms—read Oracle’s current license file) |

## Test-only dependencies

| Component | Version | License (common designation) |
|-----------|---------|----------------------------|
| [JUnit Jupiter](https://junit.org/junit5/) | 5.12.1 | EPL-2.0 |

---

## Transitive dependencies

Maven pulls in **additional libraries** (e.g. logging bridges, UI helpers, game-engine submodules for FXGL). To list everything exactly as resolved in your tree:

```bash
./mvnw dependency:list
./mvnw license:download-licenses
```

The second command requires the [license-maven-plugin](https://www.mojohaus.org/license-maven-plugin/) if you add it to `pom.xml`; it is not configured by default in this project.

---

## Your obligations

Using these components may require **retaining copyright notices**, **providing license text**, or **offering source** under certain conditions. **Your** use case (private study, portfolio demo, redistribution, commercial product) determines what applies. When in doubt, read each license or consult qualified counsel.

TuneVault’s **own** terms do not replace or narrow the licenses of third-party components.
