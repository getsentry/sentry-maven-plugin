# Changelog

## Unreleased

### Features

- Add spotless formatter maven plugin ([#14](https://github.com/getsentry/sentry-maven-plugin/pull/14))
- Add parameter to skip plugin execution ([#17](https://github.com/getsentry/sentry-maven-plugin/pull/17))
- Auto-Install Sentry and Sentry Integrations using maven build extension ([#10](https://github.com/getsentry/sentry-maven-plugin/pull/10))
- Auto-Update cli and sentry sdk version ([#12](https://github.com/getsentry/sentry-maven-plugin/pull/12))

### Dependencies

- Bump CLI from v2.19.1 to v2.21.2 ([#19](https://github.com/getsentry/sentry-maven-plugin/pull/19))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2212)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.19.1...2.21.2)
- Bump Sentry SDK from v6.25.2 to v6.32.0 ([#20](https://github.com/getsentry/sentry-maven-plugin/pull/20))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#6320)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.25.2...6.32.0)

## 0.0.4

- Download and Bundle sentry-cli with maven plugin ([#8](https://github.com/getsentry/sentry-maven-plugin/pull/8))

## 0.0.3

### Features

- Add support for building on Windows ([#7](https://github.com/getsentry/sentry-maven-plugin/pull/7))

### Fixes

- Also use `url` and `authToken` for upload command ([#7](https://github.com/getsentry/sentry-maven-plugin/pull/7))

## 0.0.2

### Features

- Initial version of sentry-maven-plugin

