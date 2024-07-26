# Changelog

## Unreleased

### Dependencies

- Bump Sentry SDK from v7.8.0 to v7.12.1 ([#78](https://github.com/getsentry/sentry-maven-plugin/pull/78), [#82](https://github.com/getsentry/sentry-maven-plugin/pull/82))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#7121)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.8.0...7.12.1)
- Bump CLI from v2.31.2 to v2.32.1 ([#76](https://github.com/getsentry/sentry-maven-plugin/pull/76))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2321)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.31.2...2.32.1)

## 0.0.8

### Fixes

- Fix `isSaas` check for telemetry ([#66](https://github.com/getsentry/sentry-maven-plugin/pull/66))
- Escape spaces in paths ([#64](https://github.com/getsentry/sentry-maven-plugin/pull/64))

### Features

- Disable source upload via `SENTRY_SKIP_SOURCE_UPLOAD` environment variable ([#65](https://github.com/getsentry/sentry-maven-plugin/pull/65))

### Dependencies

- Bump Sentry SDK from v7.3.0 to v7.8.0 ([#55](https://github.com/getsentry/sentry-maven-plugin/pull/55), [#56](https://github.com/getsentry/sentry-maven-plugin/pull/56), [#59](https://github.com/getsentry/sentry-maven-plugin/pull/59), [#72](https://github.com/getsentry/sentry-maven-plugin/pull/72))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#780)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.3.0...7.8.0)
- Bump CLI from v2.28.6 to v2.31.2 ([#58](https://github.com/getsentry/sentry-maven-plugin/pull/58), [#63](https://github.com/getsentry/sentry-maven-plugin/pull/63), [#67](https://github.com/getsentry/sentry-maven-plugin/pull/67), [#68](https://github.com/getsentry/sentry-maven-plugin/pull/68), [#69](https://github.com/getsentry/sentry-maven-plugin/pull/69), [#74](https://github.com/getsentry/sentry-maven-plugin/pull/74))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2312)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.28.6...2.31.2)

## 0.0.7

### Fixes

- Change telemetry DSN to point to production project ([#54](https://github.com/getsentry/sentry-maven-plugin/pull/54))

### Dependencies

- Bump Sentry SDK from v7.0.0 to v7.3.0 ([#40](https://github.com/getsentry/sentry-maven-plugin/pull/40), [#45](https://github.com/getsentry/sentry-maven-plugin/pull/45), [#49](https://github.com/getsentry/sentry-maven-plugin/pull/49))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#730)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.0.0...7.3.0)
- Bump CLI from v2.23.0 to v2.28.6 ([#41](https://github.com/getsentry/sentry-maven-plugin/pull/41), [#42](https://github.com/getsentry/sentry-maven-plugin/pull/42), [#43](https://github.com/getsentry/sentry-maven-plugin/pull/43), [#44](https://github.com/getsentry/sentry-maven-plugin/pull/44), [#46](https://github.com/getsentry/sentry-maven-plugin/pull/46), [#47](https://github.com/getsentry/sentry-maven-plugin/pull/47), [#48](https://github.com/getsentry/sentry-maven-plugin/pull/48), [#50](https://github.com/getsentry/sentry-maven-plugin/pull/50), [#51](https://github.com/getsentry/sentry-maven-plugin/pull/51), [#52](https://github.com/getsentry/sentry-maven-plugin/pull/52), [#53](https://github.com/getsentry/sentry-maven-plugin/pull/53))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2286)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.23.0...2.28.6)

## 0.0.6

### Features

- Report dependencies ([#22](https://github.com/getsentry/sentry-maven-plugin/pull/22))
- Send telemetry data for plugin usage ([#28](https://github.com/getsentry/sentry-maven-plugin/pull/28))
  - This will collect errors and timings of the plugin and its tasks (anonymized, except the sentry org id), so we can better understand how the plugin is performing. If you wish to opt-out of this behavior, set `<skipTelemetry>true</skipTelemetry>` in the sentry plugin configuration block.
- Add `aarch64` sentry-cli ([#39](https://github.com/getsentry/sentry-maven-plugin/pull/39))
  - This is used when the build is executed inside a docker container on an Apple silicon chip (e.g. M1)
- Allow usage of the plugin with JDK 8 ([#37](https://github.com/getsentry/sentry-maven-plugin/pull/37))
- Add `debug` flag ([#38](https://github.com/getsentry/sentry-maven-plugin/pull/38))

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

