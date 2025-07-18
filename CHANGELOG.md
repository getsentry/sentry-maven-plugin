# Changelog

## 0.7.1

### Improvements

- Log `MojoExecutionException` when running CLI fails ([#171](https://github.com/getsentry/sentry-maven-plugin/pull/171))

## 0.7.0

### Dependencies

- Bump CLI from v2.46.0 to v2.47.0 ([#166](https://github.com/getsentry/sentry-maven-plugin/pull/166))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2470)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.46.0...2.47.0)
- Bump Sentry SDK from v8.16.0 to v8.17.0 ([#167](https://github.com/getsentry/sentry-maven-plugin/pull/167))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#8170)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.16.0...8.17.0)

## 0.6.0

### Dependencies

- Bump Sentry SDK from v8.13.2 to v8.16.0 ([#164](https://github.com/getsentry/sentry-maven-plugin/pull/164))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#8160)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.13.2...8.16.0)

## 0.5.0

### Dependencies

- Bump Sentry SDK from v8.7.0 to v8.13.2 ([#147](https://github.com/getsentry/sentry-maven-plugin/pull/147), [#150](https://github.com/getsentry/sentry-maven-plugin/pull/150), [#156](https://github.com/getsentry/sentry-maven-plugin/pull/156), [#158](https://github.com/getsentry/sentry-maven-plugin/pull/158))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#8132)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.7.0...8.13.2)
- Bump CLI from v2.43.0 to v2.46.0 ([#155](https://github.com/getsentry/sentry-maven-plugin/pull/155), [#159](https://github.com/getsentry/sentry-maven-plugin/pull/159))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2460)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.43.0...2.46.0)

## 0.4.0

### Features

- Detect SDK dependency version mismatches ([#126](https://github.com/getsentry/sentry-maven-plugin/pull/126))
  - The new `validateSdkDependencyVersions` goal verifies that all of the Sentry SDK dependencies included in your `pom.xml` have consistent versions
  - We recommend enabling this goal by adding 
    ```xml
    <executions>
        <execution>
            <goals>
                <goal>validateSdkDependencyVersions</goal>
            </goals>
        </execution>
    </executions>
    ```
    within your `plugin` tag for `io.sentry:sentry-maven-plugin`
  - The build will fail in the `validate` lifecycle phase if a version mismatch is detected
  - You can opt out of this check by disabling the `validateSdkDependencyVersions` goal or by adding
    ```xml
    <configuration>
        <skipValidateSdkDependencyVersions>true</skipValidateSdkDependencyVersions>
    </configuration>
    ```
    within your `plugin` tag for `io.sentry:sentry-maven-plugin`.
    This is not recommended, as using mismatched versions of the Sentry dependencies can introduce build time or run time failures and crashes.
- Upgrade internal Sentry SDK to 8.4.0 ([#134](https://github.com/getsentry/sentry-maven-plugin/pull/134))
- Support multiple source roots ([#137](https://github.com/getsentry/sentry-maven-plugin/pull/137))
  - All source roots that are included as part of your build are now bundled together and sent to Sentry
  - This means that the Source Context feature of Sentry will work for code in all of your project's source roots
  - You can also specify additional directories to be included in the source bundle by setting the following property within your `plugin` tag for `io.sentry:sentry-maven-plugin`:
    ```xml
    <configuration>
        <additionalSourceDirsForSourceContext>
            <value>src/main/some_directory</value> 
            <value>src/main/some_other_directory</value> 
        </additionalSourceDirsForSourceContext>
    </configuration>
    ```

### Dependencies

- Bump CLI from v2.41.1 to v2.43.0 ([#125](https://github.com/getsentry/sentry-maven-plugin/pull/125), [#129](https://github.com/getsentry/sentry-maven-plugin/pull/129), [#132](https://github.com/getsentry/sentry-maven-plugin/pull/132), [#141](https://github.com/getsentry/sentry-maven-plugin/pull/141), [#144](https://github.com/getsentry/sentry-maven-plugin/pull/144))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2430)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.41.1...2.43.0)
- Bump Sentry SDK from v8.2.0 to v8.7.0 ([#130](https://github.com/getsentry/sentry-maven-plugin/pull/130), [#135](https://github.com/getsentry/sentry-maven-plugin/pull/135), [#140](https://github.com/getsentry/sentry-maven-plugin/pull/140), [#145](https://github.com/getsentry/sentry-maven-plugin/pull/145), [#146](https://github.com/getsentry/sentry-maven-plugin/pull/146))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#870)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.2.0...8.7.0)

## 0.3.0

### Dependencies

- Bump Sentry SDK from v8.1.0 to v8.2.0 ([#124](https://github.com/getsentry/sentry-maven-plugin/pull/124))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#820)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.1.0...8.2.0)

## 0.2.0

### Dependencies

- Bump Sentry SDK from v8.0.0 to v8.1.0 ([#122](https://github.com/getsentry/sentry-maven-plugin/pull/122))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#810)
  - [diff](https://github.com/getsentry/sentry-java/compare/8.0.0...8.1.0)

## 0.1.0

### Features

- Add AutoInstallStrategy for graphql-22 ([#100](https://github.com/getsentry/sentry-maven-plugin/pull/100))
- More lenient handling of empty Maven modules ([#103](https://github.com/getsentry/sentry-maven-plugin/pull/103))
  - The Maven plugin now ignores Maven modules with empty source roots and instead of failing the build simply prints a log message
  - This allows the plugin to be configured in the root POM even if it does not have sources

### Dependencies

- Bump Sentry SDK from v7.8.0 to v8.0.0 ([#78](https://github.com/getsentry/sentry-maven-plugin/pull/78), [#86](https://github.com/getsentry/sentry-maven-plugin/pull/86), [#97](https://github.com/getsentry/sentry-maven-plugin/pull/97), [#99](https://github.com/getsentry/sentry-maven-plugin/pull/99), [#104](https://github.com/getsentry/sentry-maven-plugin/pull/104), [#111](https://github.com/getsentry/sentry-maven-plugin/pull/111), [#113](https://github.com/getsentry/sentry-maven-plugin/pull/113), [#115](https://github.com/getsentry/sentry-maven-plugin/pull/115), [#120](https://github.com/getsentry/sentry-maven-plugin/pull/120))
  - [changelog](https://github.com/getsentry/sentry-java/blob/main/CHANGELOG.md#800)
  - [diff](https://github.com/getsentry/sentry-java/compare/7.8.0...8.0.0)
- Bump CLI from v2.31.2 to v2.41.1 ([#76](https://github.com/getsentry/sentry-maven-plugin/pull/76), [#85](https://github.com/getsentry/sentry-maven-plugin/pull/85), [#87](https://github.com/getsentry/sentry-maven-plugin/pull/87), [#90](https://github.com/getsentry/sentry-maven-plugin/pull/90), [#91](https://github.com/getsentry/sentry-maven-plugin/pull/91), [#96](https://github.com/getsentry/sentry-maven-plugin/pull/96), [#98](https://github.com/getsentry/sentry-maven-plugin/pull/98), [#101](https://github.com/getsentry/sentry-maven-plugin/pull/101), [#102](https://github.com/getsentry/sentry-maven-plugin/pull/102), [#109](https://github.com/getsentry/sentry-maven-plugin/pull/109), [#114](https://github.com/getsentry/sentry-maven-plugin/pull/114), [#119](https://github.com/getsentry/sentry-maven-plugin/pull/119))
  - [changelog](https://github.com/getsentry/sentry-cli/blob/master/CHANGELOG.md#2411)
  - [diff](https://github.com/getsentry/sentry-cli/compare/2.31.2...2.41.1)

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

