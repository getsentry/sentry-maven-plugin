# Changelog

## Unreleased

### Features

- Report dependencies ([#22](https://github.com/getsentry/sentry-maven-plugin/pull/22))
- Send telemetry data for plugin usage ([#28](https://github.com/getsentry/sentry-maven-plugin/pull/28))
  - This will collect errors and timings of the plugin and its tasks (anonymized, except the sentry org id), so we can better understand how the plugin is performing. If you wish to opt-out of this behavior, set `<skipTelemetry>true</skipTelemetry>` in the sentry plugin configuration block.
- Add `aarch64` sentry-cli ([#39](https://github.com/getsentry/sentry-maven-plugin/pull/39))
  - This is used when the build is executed inside a docker container on an Apple silicon chip (e.g. M1)
- Allow usage of the plugin with JDK 8 ([#37](https://github.com/getsentry/sentry-maven-plugin/pull/37))

### Dependencies

- Bump Sentry SDK from v6.32.0 to v7.0.0 ([#23](https://github.com/getsentry/sentry-maven-plugin/pull/23), [#24](https://github.com/getsentry/sentry-maven-plugin/pull/24), [#29](https://github.com/getsentry/sentry-maven-plugin/pull/29), [#32](https://github.com/getsentry/sentry-maven-plugin/pull/32))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#700)
  - [diff](https://github.com/getsentry/sentry-java/compare/6.32.0...7.0.0)
- Bump CLI from v2.21.2 to v2.23.0 ([#25](https://github.com/getsentry/sentry-maven-plugin/pull/25), [#27](https://github.com/getsentry/sentry-maven-plugin/pull/27), [#30](https://github.com/getsentry/sentry-maven-plugin/pull/30), [#31](https://github.com/getsentry/sentry-maven-plugin/pull/31), [#35](https://github.com/getsentry/sentry-maven-plugin/pull/35))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2230)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.21.2...2.23.0)

## 0.0.5

### Features

- Add parameter to skip plugin execution ([#17](https://github.com/getsentry/sentry-maven-plugin/pull/17))
- Auto-Install Sentry and Sentry integrations using Maven build extension ([#10](https://github.com/getsentry/sentry-maven-plugin/pull/10))
- Auto-Update `sentry-cli` and Sentry Java SDK version ([#12](https://github.com/getsentry/sentry-maven-plugin/pull/12))

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

