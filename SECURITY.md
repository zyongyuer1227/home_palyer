# Security Policy

## Reporting

Please report security issues privately to the project maintainer instead of opening a public issue.

## Scope

Security-sensitive areas include:

- Backend server addresses and credentials stored on device.
- Playback URLs, tokens, and NAS file paths.
- Release signing keys and keystore passwords.
- Logs exported for troubleshooting.

## Release Signing

Release keystores and passwords must stay outside git. This repository only contains the Gradle wiring needed to read signing values from local-only properties.
