![Maintained][maintained-badge]
[![build](https://github.com/hopskipnfall/EmuLinkerSF-Netosuma/actions/workflows/maven.yml/badge.svg)](https://github.com/hopskipnfall/EmuLinkerSF-Netosuma/actions/workflows/maven.yml)
[![Make a pull request][prs-badge]][prs]

[![Watch on GitHub][github-watch-badge]][github-watch]
[![Star on GitHub][github-star-badge]][github-star]
[![Tweet][twitter-badge]][twitter]

# EmuLinkerSF-Netosuma

EmuLinkerSF-Netosuma (ネトスマ) is a SSB64-specific (originally just Japan-focused) fork of [EmulinkerSF](https://github.com/God-Weapon/EmuLinkerSF), which again is a fork of [EmuLinker](https://github.com/monospacesoftware/emulinker).

The aim of this repository is to:

 - Add new features useful to SSB64 netplay.
 - Modernize the codebase and fix vulnerabilities while maintaining or increasing the level of performance.

EmuLinkerSF-Netosuma is maintained by [鵺 (nue)](https://twitter.com/nuenuessb).

## Development

From the emulinker/ directory, the following commands are supported:

| Command               | Description                                                                                             |
| --------------------  | ------------------------------------------------------------------------------------------------------- |
| `mvn compile`         | Compile the code. Running this command also patches [ErrorProne](https://errorprone.info/) suggestions. |
| `mvn spotless:apply`  | Run the formatter.                                                                                      |
| `mvn spotless:check`  | Run the linter.                                                                                         |
| `mvn test`            | Run tests.                                                                                              |
| `mvn assembly:single` | Build the jar.                                                                                          |
| `mvn exec:java`       | Run the server locally.                                                                                 |

Note: You will need to have Maven installed.

## Deployment

```java
// TODO(nue): Write deployment steps here.
```

Note: If you use non-ASCII characters in the `conf/language.properties` file, you need to run with at least Java 9 for the characters to appear correctly.

### Docker

Docker support is WIP.

To run locally with Docker:

```shell
docker-compose up -d
```

To run locally with Docker and a local Graphite instance to monitor metrics (note: you need to also set `metrics.enabled=true` in emulinker.cfg):

```
docker-compose --profile=debug up -d
```

You can then access the Graphite dashboard at http://localhost/dashboard

[prs-badge]: https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square
[prs]: http://makeapullrequest.com
[github-watch-badge]: https://img.shields.io/github/watchers/hopskipnfall/EmuLinkerSF-Netosuma.svg?style=social
[github-watch]: https://github.com/hopskipnfall/EmuLinkerSF-Netosuma/watchers
[github-star-badge]: https://img.shields.io/github/stars/hopskipnfall/EmuLinkerSF-Netosuma.svg?style=social
[github-star]: https://github.com/hopskipnfall/EmuLinkerSF-Netosuma/stargazers
[twitter]: https://twitter.com/intent/tweet?text=https://github.com/hopskipnfall/EmuLinkerSF-Netosuma%20%F0%9F%91%8D
[twitter-badge]: https://img.shields.io/twitter/url/https/github.com/hopskipnfall/EmuLinkerSF-Netosuma.svg?style=social
[maintained-badge]: https://img.shields.io/badge/maintained-yes-brightgreen
