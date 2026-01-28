# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.2.4](https://github.com/JNZader/apigen/compare/v3.2.3...v3.2.4) (2026-01-28)

### Bug Fixes

* **codegen:** align C# and Java test generators with actual entity/service signatures ([cf48acb](https://github.com/JNZader/apigen/commit/cf48acb88dc412b40e08894b651009dbc030be24))

## [3.2.3](https://github.com/JNZader/apigen/compare/v3.2.2...v3.2.3) (2026-01-28)

### Bug Fixes

* **codegen:** fix C# test generators and pitest Gradle 9.x compatibility ([00cb9a2](https://github.com/JNZader/apigen/commit/00cb9a28aec9fd1c23c9489c91cb3a4382844c42))

## [3.2.2](https://github.com/JNZader/apigen/compare/v3.2.1...v3.2.2) (2026-01-28)

### Bug Fixes

* **codegen:** resolve CI failures for pitest plugin and C# tests ([60d2d77](https://github.com/JNZader/apigen/commit/60d2d7790ccf19678d51d2a7218a207e131996b3))

## [3.2.1](https://github.com/JNZader/apigen/compare/v3.2.0...v3.2.1) (2026-01-28)

### Bug Fixes

* **codegen:** resolve CI compilation failures for generated projects ([7e3543f](https://github.com/JNZader/apigen/commit/7e3543fa0098d206b2f31d1f0ee59a122147f5e8))

## [3.2.0](https://github.com/JNZader/apigen/compare/v3.1.0...v3.2.0) (2026-01-28)

### Features

* **codegen:** complete test generation for all 9 supported languages ([0f026bf](https://github.com/JNZader/apigen/commit/0f026bf5db62d1af1763830a3aa6c2b5e5f1eb51))

## [3.1.0](https://github.com/JNZader/apigen/compare/v3.0.7...v3.1.0) (2026-01-28)

### Features

* **codegen:** add comprehensive test generators for 100% coverage ([815711a](https://github.com/JNZader/apigen/commit/815711abdfdb866d9110dd1997d3cd9d278fa1dc))

### Bug Fixes

* **server:** enable UNIT_TESTS and INTEGRATION_TESTS feature flags ([ba1fa3e](https://github.com/JNZader/apigen/commit/ba1fa3e785d517047907996500ee1164ccb2e485))

## [3.0.7](https://github.com/JNZader/apigen/compare/v3.0.6...v3.0.7) (2026-01-28)

### Bug Fixes

* **server:** add default value getters for RateLimitConfig ([7a231f7](https://github.com/JNZader/apigen/commit/7a231f7622a9932477af9e50165de373e44110af))

## [3.0.6](https://github.com/JNZader/apigen/compare/v3.0.5...v3.0.6) (2026-01-28)

### Bug Fixes

* **build:** restore jitpack.yml and add JitPack badge to README ([ae1ce68](https://github.com/JNZader/apigen/commit/ae1ce680094cca1f63e0367b2442cc0a672b755a))

## [3.0.5](https://github.com/JNZader/apigen/compare/v3.0.4...v3.0.5) (2026-01-28)

### Bug Fixes

* **codegen:** revert generated projects to JitPack (no auth required) ([294a19b](https://github.com/JNZader/apigen/commit/294a19b51e7c7ce2578bf965f216f8616823f22d))

## [3.0.4](https://github.com/JNZader/apigen/compare/v3.0.3...v3.0.4) (2026-01-28)

### Bug Fixes

* **codegen:** remove MISE_JAVA_HOME env var that fails on Windows ([3e6b055](https://github.com/JNZader/apigen/commit/3e6b0550435a021e30c7c5ca8f02af8086fb1df2))
* **codegen:** use gradlew without ./ prefix for Windows compatibility ([16dbe25](https://github.com/JNZader/apigen/commit/16dbe252ce358e55e30eb636c28e049887b99dfe))

## [3.0.3](https://github.com/JNZader/apigen/compare/v3.0.2...v3.0.3) (2026-01-28)

### Bug Fixes

* **docs:** update APiGen web URL from apigen-studio to apigen-web ([6f3a933](https://github.com/JNZader/apigen/commit/6f3a933424c965ef1751e89ea0894198c8d2657a))

## [3.0.2](https://github.com/JNZader/apigen/compare/v3.0.1...v3.0.2) (2026-01-28)

### Bug Fixes

* **docker:** add apigen-exceptions module to root Dockerfile ([d854365](https://github.com/JNZader/apigen/commit/d854365908a5d7b90a687319bed90806abbaed29))

## [3.0.1](https://github.com/JNZader/apigen/compare/v3.0.0...v3.0.1) (2026-01-28)

### Bug Fixes

* **docker:** add apigen-exceptions module to Dockerfile ([ec6402b](https://github.com/JNZader/apigen/commit/ec6402b7c6937ffacec62e4655b0342784a7c39f))

## [3.0.0](https://github.com/JNZader/apigen/compare/v2.27.0...v3.0.0) (2026-01-28)

### ⚠ BREAKING CHANGES

* **publishing:** Package coordinates changed from
com.github.jnzader.apigen:apigen-* to com.jnzader:apigen-*
Version format changed from v2.x.x to 2.x.x (no 'v' prefix)

### Features

* **publishing:** migrate from JitPack to GitHub Packages ([3b60b1b](https://github.com/JNZader/apigen/commit/3b60b1bebd382d1fd3d55433582db76c31185e8b))

## [2.27.0](https://github.com/JNZader/apigen/compare/v2.26.6...v2.27.0) (2026-01-28)

### Features

* **exceptions:** add apigen-exceptions module for lightweight exception handling ([67f668e](https://github.com/JNZader/apigen/commit/67f668e03d270c34017c1d3259920af2f1d99d70))

## [2.26.6](https://github.com/JNZader/apigen/compare/v2.26.5...v2.26.6) (2026-01-28)

### Bug Fixes

* **server:** keep cache/hateoas deps, only exclude JPA/Hibernate ([e9b81e7](https://github.com/JNZader/apigen/commit/e9b81e7ec0cc35ca158f0ffed717943f53ee9433))

## [2.26.5](https://github.com/JNZader/apigen/compare/v2.26.4...v2.26.5) (2026-01-28)

### Bug Fixes

* **server:** exclude JPA from apigen-core to fix Koyeb deployment ([9cf8372](https://github.com/JNZader/apigen/commit/9cf837208e6662918b6d3a8b40e8760d70f3eaa2))

## [2.26.4](https://github.com/JNZader/apigen/compare/v2.26.3...v2.26.4) (2026-01-28)

### Bug Fixes

* **server:** remove invalid auto-config exclusion ([b58cf33](https://github.com/JNZader/apigen/commit/b58cf334d4a591198a4ecd0a119ae07a02fc1861))

## [2.26.3](https://github.com/JNZader/apigen/compare/v2.26.2...v2.26.3) (2026-01-28)

### Bug Fixes

* **server:** add comprehensive database auto-config exclusions ([832c288](https://github.com/JNZader/apigen/commit/832c288e2322a5fd54da45a23ed61c964e932507))

## [2.26.2](https://github.com/JNZader/apigen/compare/v2.26.1...v2.26.2) (2026-01-28)

### Bug Fixes

* **server:** exclude DataSource auto-config for stateless server ([14fb38b](https://github.com/JNZader/apigen/commit/14fb38b755a14ae11d399fb3540187a3ded2ca74))
* **server:** use excludeName instead of exclude for auto-config ([867b03b](https://github.com/JNZader/apigen/commit/867b03bc589ebc8acadcba385cd1ce15a78d03dd))

## [2.26.1](https://github.com/JNZader/apigen/compare/v2.26.0...v2.26.1) (2026-01-28)

### Bug Fixes

* **server:** rename GlobalExceptionHandler to avoid bean conflict ([ac3a9b0](https://github.com/JNZader/apigen/commit/ac3a9b009c275f431bd9832af259c2174de0fefc))

## [2.26.0](https://github.com/JNZader/apigen/compare/v2.25.0...v2.26.0) (2026-01-28)

### Features

* standardize error handling (RFC 7807) and improve README generator ([f44d02a](https://github.com/JNZader/apigen/commit/f44d02a33fd421576709889da5bb03482930102d))

### Bug Fixes

* **gateway:** escape generics in javadoc comments ([b0747f3](https://github.com/JNZader/apigen/commit/b0747f32a862811fc90849c4a78830f0ca93d4d9))

## [2.25.0](https://github.com/JNZader/apigen/compare/v2.24.5...v2.25.0) (2026-01-27)

### Features

* **readme:** enhance generated README with entity docs and endpoints ([0d53418](https://github.com/JNZader/apigen/commit/0d534187262675d15f5d8654a0116a11d6ae8bb1))

## [2.24.5](https://github.com/JNZader/apigen/compare/v2.24.4...v2.24.5) (2026-01-27)

### Bug Fixes

* **github:** add httpcore5-reactive dependency for WebClient ([f44dcea](https://github.com/JNZader/apigen/commit/f44dceaa408ff1037e314af0adbeea79ecd5d6b5))

## [2.24.4](https://github.com/JNZader/apigen/compare/v2.24.3...v2.24.4) (2026-01-27)

### Bug Fixes

* **github:** resolve URI validation error with Netty 4.2.x ([b08b463](https://github.com/JNZader/apigen/commit/b08b463999ac3ae4340cf262cb5c746c91891478))
* **github:** use Apache HttpClient 5 instead of Netty for WebClient ([fcce9fa](https://github.com/JNZader/apigen/commit/fcce9fac8fced1e78d22ddde867fcd84b9d2a366))

## [2.24.3](https://github.com/JNZader/apigen/compare/v2.24.2...v2.24.3) (2026-01-27)

### Bug Fixes

* **github:** allow null values for optional fields in PushProjectRequest ([f008431](https://github.com/JNZader/apigen/commit/f00843105ec488a0048ef54252bdada46f10d85b))

## [2.24.2](https://github.com/JNZader/apigen/compare/v2.24.1...v2.24.2) (2026-01-27)

### Bug Fixes

* **github:** allow null values for optional boolean fields in CreateRepoRequest ([86dbae3](https://github.com/JNZader/apigen/commit/86dbae38f7a9033b2b2e4ead2c11f0c0238e6506))

## [2.24.1](https://github.com/JNZader/apigen/compare/v2.24.0...v2.24.1) (2026-01-27)

### Bug Fixes

* **github:** use SameSite=None for cross-origin cookie support ([1a39bf0](https://github.com/JNZader/apigen/commit/1a39bf09102b75f780e44538c58da210eb1271d4))

## [2.24.0](https://github.com/JNZader/apigen/compare/v2.23.0...v2.24.0) (2026-01-27)

### Features

* **github:** use public_repo scope by default for fewer permissions ([8e1160d](https://github.com/JNZader/apigen/commit/8e1160de4f30bc01094c9b03451e618efb673bfa))

## [2.23.0](https://github.com/JNZader/apigen/compare/v2.22.1...v2.23.0) (2026-01-27)

### Features

* **server:** add GitHub repository listing endpoint ([a57535d](https://github.com/JNZader/apigen/commit/a57535ddcbdbbd203aba84dd1cfc628a23b1eb11))

### Bug Fixes

* **github:** redirect to frontend after OAuth callback ([b3f882f](https://github.com/JNZader/apigen/commit/b3f882f95dc27b078f9cff88e47022f00ff72fa4))

## [2.22.1](https://github.com/JNZader/apigen/compare/v2.22.0...v2.22.1) (2026-01-27)

### Bug Fixes

* **github:** replace JsonNode with Map to fix Jackson 3.x compatibility ([ba765a3](https://github.com/JNZader/apigen/commit/ba765a3b83e79396c1af8e56383f7d0ec442cc13))

## [2.22.0](https://github.com/JNZader/apigen/compare/v2.21.0...v2.22.0) (2026-01-27)

### Features

* **server:** add Spring Boot Actuator for health checks ([2e7765f](https://github.com/JNZader/apigen/commit/2e7765ff2c37f8301daddf41d69226939fec0414))

## [2.21.0](https://github.com/JNZader/apigen/compare/v2.20.1...v2.21.0) (2026-01-27)

### Features

* **codegen:** add Postman collection for all languages ([39e4f1c](https://github.com/JNZader/apigen/commit/39e4f1cd576fbf2b0a439d7c5d8c60495dbd8ded))
* **codegen:** use language-specific ports in Postman collection ([b271629](https://github.com/JNZader/apigen/commit/b271629cf1b2356371b9d5193bc7dbb828720eb7))

### Bug Fixes

* **codegen:** sync framework names and version defaults with frontend ([346fba6](https://github.com/JNZader/apigen/commit/346fba654ccdaa2b76519fadb1a57b0f7474bc15))
* **codegen:** update tests to match new versions and framework names ([b6f426b](https://github.com/JNZader/apigen/commit/b6f426bf4e92bbe233265b530915b9fecdfb88d6))

## [2.20.1](https://github.com/JNZader/apigen/compare/v2.20.0...v2.20.1) (2026-01-27)

### Bug Fixes

* **server:** use Boolean wrappers for all FeaturesConfig fields to handle null JSON values ([c197595](https://github.com/JNZader/apigen/commit/c19759505f2b052786e3acbdcc695ac40cb642bc))

## [2.20.0](https://github.com/JNZader/apigen/compare/v2.19.1...v2.20.0) (2026-01-27)

### Features

* **server:** add DX features support to generation request ([96862ab](https://github.com/JNZader/apigen/commit/96862ab098b96d68c66f0c90a8e062bf31b22942))

### Bug Fixes

* **server:** use Boolean wrappers for DX features to handle null JSON values ([973127d](https://github.com/JNZader/apigen/commit/973127d7c1deaf95c79432a987fea2234d7e9da1))

## [2.19.1](https://github.com/JNZader/apigen/compare/v2.19.0...v2.19.1) (2026-01-27)

### Bug Fixes

* **graphql:** update DataLoader API for java-dataloader 6.0.0 ([abd688e](https://github.com/JNZader/apigen/commit/abd688e65f52c9d58ac9629f02514c82cb3a64b4))
* **graphql:** update DataLoaderOptions API for java-dataloader 6.0.0 ([b1364c5](https://github.com/JNZader/apigen/commit/b1364c5b16281ca7237230b890d2b60aff66a678))

### Build System

* **deps:** bump com.graphql-java:java-dataloader from 3.4.0 to 6.0.0 ([7ffc3a0](https://github.com/JNZader/apigen/commit/7ffc3a07228e26a931ef612d05fc10e936e1821c))

## [2.19.0](https://github.com/JNZader/apigen/compare/v2.18.2...v2.19.0) (2026-01-27)

### Features

* **codegen:** add developer experience features for all 9 generators ([64793ca](https://github.com/JNZader/apigen/commit/64793caae167596d6e68d4c690895e0be270395b))

### Bug Fixes

* **graphql:** remove invalid .build() calls from DataLoaderOptions ([24ae5e3](https://github.com/JNZader/apigen/commit/24ae5e3b6863569aebc4bc74a958898a23a3167e))

## [2.18.2](https://github.com/JNZader/apigen/compare/v2.18.1...v2.18.2) (2026-01-27)

### Build System

* **deps:** bump protobufVersion from 4.31.1 to 4.33.4 ([7d8d8b4](https://github.com/JNZader/apigen/commit/7d8d8b404ab9bf76265b96e72265789cf5cdd1f6))

## [2.18.1](https://github.com/JNZader/apigen/compare/v2.18.0...v2.18.1) (2026-01-27)

### Documentation

* update documentation to v2.18.0 and fix outdated references ([d48a1c7](https://github.com/JNZader/apigen/commit/d48a1c70c341436660f20a58fbb73b51544db41b))
* update README with 9-language support and fix paths ([77b6045](https://github.com/JNZader/apigen/commit/77b604583f7c348513621bcd0c860b82d5b9851d))

### Build System

* **deps:** bump grpcVersion from 1.72.0 to 1.78.0 ([2b39632](https://github.com/JNZader/apigen/commit/2b39632a01455d362ce05bbbd1350ec6f51fc28e))
* **deps:** bump org.springframework.boot:spring-boot-dependencies ([d6c7e9c](https://github.com/JNZader/apigen/commit/d6c7e9c8ef2e6ec845ffefc457069f522c03c22d))
* **deps:** bump org.springframework.boot:spring-boot-gradle-plugin ([39e9846](https://github.com/JNZader/apigen/commit/39e9846606cfc79afb437fd58206b58b818adb15))

### CI/CD

* **deps:** bump actions/setup-python from 5 to 6 ([f02441a](https://github.com/JNZader/apigen/commit/f02441a3f315d9167bec345a4f48a5ddf829d1c1))
* **deps:** bump actions/upload-pages-artifact from 3 to 4 ([68bba48](https://github.com/JNZader/apigen/commit/68bba4895fcc713f58f799a9e30a8283ff4ddf63))
* **deps:** bump gradle/actions from 4 to 5 ([b1563aa](https://github.com/JNZader/apigen/commit/b1563aaedd2e70f83bf617c1bb2b2480f68b57f2))

## [2.18.0](https://github.com/JNZader/apigen/compare/v2.17.0...v2.18.0) (2026-01-25)

### Features

* **codegen,server:** implement reserved functionality for unused parameters ([a2a4f88](https://github.com/JNZader/apigen/commit/a2a4f880bc8e0b71ded453e69b5b4f0175027e19))

### Bug Fixes

* **codegen,security:** resolve high severity SonarQube issues ([77dfa0e](https://github.com/JNZader/apigen/commit/77dfa0e1ab18ebd3baecb8b460647ab300ce3cea))
* **codegen,server:** add null checks to prevent NullPointerExceptions ([089ebc2](https://github.com/JNZader/apigen/commit/089ebc2d2edc762f25dec27bf7dca02b238e23fa))
* **codegen,server:** resolve all SonarQube issues ([015e5c2](https://github.com/JNZader/apigen/commit/015e5c288b6fd6122b0883e3e9549825f648df04))
* **codegen,server:** resolve SonarQube MAJOR issues ([72f92b8](https://github.com/JNZader/apigen/commit/72f92b801cd4c400b328319e573bd0129e9cc9a3))
* **codegen:** fix SuppressWarnings annotation in Kotlin generator ([437c6dc](https://github.com/JNZader/apigen/commit/437c6dc393a456257fa6267258b787a603367532))
* **codegen:** resolve S125 and S2068 SonarQube issues ([0154a60](https://github.com/JNZader/apigen/commit/0154a60fa3166519f81bf7cb6ca562969758d763))
* **core,server:** resolve additional high severity SonarQube issues ([e730037](https://github.com/JNZader/apigen/commit/e730037fe383a1784280404bfe0a523c53ad6345))

### Code Refactoring

* **codegen:** reduce code duplication across generators ([3a9fcd4](https://github.com/JNZader/apigen/commit/3a9fcd4c710569540e98057f66d7544490e78e4d))

## [2.17.0](https://github.com/JNZader/apigen/compare/v2.16.0...v2.17.0) (2026-01-24)

### Features

* **codegen:** add native Feature Pack 2025 generators for Go/Chi ([02e77bf](https://github.com/JNZader/apigen/commit/02e77bfd53eea1782d64e28cdc3c5757fb3a65cf))

## [2.16.0](https://github.com/JNZader/apigen/compare/v2.15.1...v2.16.0) (2026-01-24)

### Features

* **codegen:** add test generator for Go/Chi projects ([4d4efe8](https://github.com/JNZader/apigen/commit/4d4efe8dd5d1d312be519a85fd5994c6fcc86599))
* **codegen:** add test generators for all language frameworks ([ac0d5d3](https://github.com/JNZader/apigen/commit/ac0d5d31e9f5c9bab5b16ac6121365ea4002e907))

## [2.15.1](https://github.com/JNZader/apigen/compare/v2.15.0...v2.15.1) (2026-01-24)

### Bug Fixes

* **deps:** upgrade netty and rhino for security vulnerabilities ([68a8bc8](https://github.com/JNZader/apigen/commit/68a8bc8b1180b12fc17f9e28524122e5d8e7ac9f))

## [2.15.0](https://github.com/JNZader/apigen/compare/v2.14.0...v2.15.0) (2026-01-24)

### Features

* **codegen:** add Feature Pack 2025 to all generators ([172e3cc](https://github.com/JNZader/apigen/commit/172e3cc4759016f04256f77e3debd3284455bb3c))

## [2.14.0](https://github.com/JNZader/apigen/compare/v2.13.0...v2.14.0) (2026-01-24)

### Features

* **codegen:** add JWT auth and rate limiting for all generators ([364de26](https://github.com/JNZader/apigen/commit/364de263cfd00b68216f71cb01c35f8f0792f112))

### Bug Fixes

* **codegen:** correct governor crate wait_time_from API usage ([cc29668](https://github.com/JNZader/apigen/commit/cc2966804a9edfaf385c255b3ed94f4296b122d9))

## [2.13.0](https://github.com/JNZader/apigen/compare/v2.12.0...v2.13.0) (2026-01-24)

### Features

* **codegen:** add Feature Pack 2025 with 7 new generators ([64226ef](https://github.com/JNZader/apigen/commit/64226eff3e1391288f097fdcf9b97d858f640f86))

## [2.12.0](https://github.com/JNZader/apigen/compare/v2.11.0...v2.12.0) (2026-01-24)

### Features

* **codegen:** add Rust/Axum project generator ([0fc1571](https://github.com/JNZader/apigen/commit/0fc1571e78fa4baca25ffc52d57aba5cf9cd4fde))

## [2.11.0](https://github.com/JNZader/apigen/compare/v2.10.0...v2.11.0) (2026-01-24)

### Features

* **codegen:** add Go/Chi Router generator with modular features ([1d7f528](https://github.com/JNZader/apigen/commit/1d7f528888aa974dd8d56d6a5a8bb641c9912155))

### Bug Fixes

* **codegen:** resolve Go/Chi generated project compilation errors ([5efbdf4](https://github.com/JNZader/apigen/commit/5efbdf480882bc80aec13fb5ed8803f64aeb7257))
* **server:** support explicit basePackage for Go module paths ([b0398ff](https://github.com/JNZader/apigen/commit/b0398ffa14e98ad8f48dcef61e8780bd66400445))

### CI/CD

* add Go/Chi project compilation test job ([f54aa44](https://github.com/JNZader/apigen/commit/f54aa44ac41d5dc5fce98687cf995a39b9c3c250))

## [2.10.0](https://github.com/JNZader/apigen/compare/v2.9.0...v2.10.0) (2026-01-24)

### Features

* **codegen:** add Go/Gin project generator ([a131fd0](https://github.com/JNZader/apigen/commit/a131fd0284e377436da2f337ef950672ed021b6d))

### Bug Fixes

* **codegen:** fix Go and TypeScript CI compilation errors ([6ab795a](https://github.com/JNZader/apigen/commit/6ab795a757f4ff040b92aed19bda8966c69a7cc9))
* **codegen:** handle null length/scale/precision in type mappers ([e991219](https://github.com/JNZader/apigen/commit/e99121994b20b78314ce9fe3a7f5c7b5ab32a95b))
* **codegen:** resolve generated project compilation issues ([54e7f08](https://github.com/JNZader/apigen/commit/54e7f088c4f95885b1b5ad17ecaef0600fa67ba8))
* **codegen:** resolve Go and TypeScript generated project issues ([3be7777](https://github.com/JNZader/apigen/commit/3be7777efffb23887f7377c33d6ef95f969f8a13))
* **codegen:** resolve remaining generated project issues ([be36fb2](https://github.com/JNZader/apigen/commit/be36fb2b8b55a3927aff0710ac00c14ca5188f27))

### CI/CD

* add generated project tests for all languages ([e7535b9](https://github.com/JNZader/apigen/commit/e7535b9414039fc1a52814f2137ab0d621cac38a))

## [2.9.0](https://github.com/JNZader/apigen/compare/v2.8.0...v2.9.0) (2026-01-24)

### Features

* **codegen:** add TypeScript/NestJS project generator ([17092d8](https://github.com/JNZader/apigen/commit/17092d83aaa3b7f0d170fcacb9907956da02d4b4))

### Bug Fixes

* **codegen:** update display names and tests for Spring Boot 4.x ([3d417eb](https://github.com/JNZader/apigen/commit/3d417eba89eb4a26b7b69b1e40fa5763bc62a872))

## [2.8.0](https://github.com/JNZader/apigen/compare/v2.7.0...v2.8.0) (2026-01-24)

### Features

* **codegen:** add PHP/Laravel project generator ([574b3c1](https://github.com/JNZader/apigen/commit/574b3c11f6b7765f63603c9d2088de4dec322875))

## [2.7.0](https://github.com/JNZader/apigen/compare/v2.6.0...v2.7.0) (2026-01-24)

### Features

* **codegen:** add C#/ASP.NET Core project generator ([f2353ea](https://github.com/JNZader/apigen/commit/f2353eaf74a835ada815908c65c0c1458cddbe4e))
* **codegen:** add Python/FastAPI project generator ([72c96a1](https://github.com/JNZader/apigen/commit/72c96a1f65d6a0917a8fe31a59071100417182cf))

### Bug Fixes

* **ci:** use .NET 8.0 SDK and fix architecture tests ([1693594](https://github.com/JNZader/apigen/commit/1693594d3ec2c01aab4b9c8c8993717232fb873d))
* **ci:** use valid GitHub Actions versions in docs workflow ([ceee388](https://github.com/JNZader/apigen/commit/ceee388f7a1d77f055386b4cc6c346e5953611fe))
* **codegen:** add AutoMapper DI extensions package for .NET 8.0 ([4d7bc79](https://github.com/JNZader/apigen/commit/4d7bc79faac0b34861d5b22e3ee5a19b57aea1c4))
* **codegen:** cap Kotlin JvmTarget at JVM_21 ([11e6ccd](https://github.com/JNZader/apigen/commit/11e6ccd8f626065ca1e3f892d3181e87412702a7))
* **codegen:** language-aware project generation and integration tests ([85c721c](https://github.com/JNZader/apigen/commit/85c721cdcdad40c2a9c9541ca6221cea8d5cd7a8))
* **codegen:** revert C# to .NET 9.0 for CI stability ([e7bfb20](https://github.com/JNZader/apigen/commit/e7bfb20cf726c16bdd6ec3258f2b94c24cc2c75f))
* **codegen:** use .NET 8.0 for CI stability ([069198b](https://github.com/JNZader/apigen/commit/069198bc7477967d7ec68dfea2855894b0f76cfb))
* **deps:** upgrade logback to 1.5.25 for CVE-2026-1225 ([3469f67](https://github.com/JNZader/apigen/commit/3469f67918d3592749ae773a0a0abbfc349110f6))

### CI/CD

* add timeouts, concurrency, and update all workflows ([239cdef](https://github.com/JNZader/apigen/commit/239cdef5b32b0bcc8307be84448267ad4493cbb5))
* remove continue-on-error and update .NET SDK to 10.0 ([255b620](https://github.com/JNZader/apigen/commit/255b6207f37de2de6d3ac1241d96904fa74f8a7d))
* separate integration tests into per-language jobs ([62e54df](https://github.com/JNZader/apigen/commit/62e54df90c12198fe6e34043e1c2bbf35bbf708f))

## [2.6.0](https://github.com/JNZader/apigen/compare/v2.5.10...v2.6.0) (2026-01-23)

### Features

* **codegen:** add Kotlin/Spring Boot project generator ([4c95556](https://github.com/JNZader/apigen/commit/4c9555664449f42bf8b4d86033db35e0cbcd690e))

## [2.5.10](https://github.com/JNZader/apigen/compare/v2.5.9...v2.5.10) (2026-01-23)

### Code Refactoring

* **codegen:** add multi-language architecture with generator registry ([5823188](https://github.com/JNZader/apigen/commit/58231883c87f25c4235d305f30646f7361d90179))

### CI/CD

* make artifact uploads continue on error to handle quota limits ([eeeac27](https://github.com/JNZader/apigen/commit/eeeac27758894694c80cb84be55e50dd20c0456f))

## [2.5.9](https://github.com/JNZader/apigen/compare/v2.5.8...v2.5.9) (2026-01-23)

### Code Refactoring

* replace apigen-example module with apigen-examples folder ([cf13cce](https://github.com/JNZader/apigen/commit/cf13ccec5012e3e950551a28d282793b7cdb3707))

### CI/CD

* add cache cleanup workflow to prevent storage quota issues ([e8750b7](https://github.com/JNZader/apigen/commit/e8750b72dc62f2474a1e6fce405a2203c0887b07))
* add keep-alive workflow to prevent Koyeb instance sleep ([4af91e0](https://github.com/JNZader/apigen/commit/4af91e0321eeb15de54fec45f21b1173b25fc444))
* reduce artifact retention to 1 day to avoid storage quota issues ([bc6858d](https://github.com/JNZader/apigen/commit/bc6858d64da60ce538a18b079f9af3b3f74f8e4f))

## [2.5.8](https://github.com/JNZader/apigen/compare/v2.5.7...v2.5.8) (2026-01-23)

### Bug Fixes

* **security:** remove duplicate JPA annotations to prevent BeanDefinitionOverrideException ([4f9f3db](https://github.com/JNZader/apigen/commit/4f9f3dbb928a0dddf9cfa2104af7cca404109703)), closes [#64](https://github.com/JNZader/apigen/issues/64)

## [2.5.7](https://github.com/JNZader/apigen/compare/v2.5.6...v2.5.7) (2026-01-23)

### Bug Fixes

* **codegen:** add @EnableJpaRepositories and @EntityScan to generated Application class ([89df53b](https://github.com/JNZader/apigen/commit/89df53bf9f7df9623f2b013694251dc3f8083084))
* **test:** correct ZIP extraction path for compilation test ([f24b6fc](https://github.com/JNZader/apigen/commit/f24b6fc840ddfe566aa898d4921cf79c47c32539))

## [2.5.6](https://github.com/JNZader/apigen/compare/v2.5.5...v2.5.6) (2026-01-23)

### Code Refactoring

* **security:** migrate JwtService to Jackson 3 ([30b5684](https://github.com/JNZader/apigen/commit/30b5684bfb02fe7042b8aab5779c339966ce25f3))

## [2.5.5](https://github.com/JNZader/apigen/compare/v2.5.4...v2.5.5) (2026-01-23)

### Bug Fixes

* **security:** add @EntityScan for JPA entity scanning ([3c37155](https://github.com/JNZader/apigen/commit/3c37155c021406c49586c268e90101d93b1b5616))

## [2.5.4](https://github.com/JNZader/apigen/compare/v2.5.3...v2.5.4) (2026-01-23)

### Bug Fixes

* **security:** correct JPA repositories base package ([48fba87](https://github.com/JNZader/apigen/commit/48fba87da0a8e305fa2b75b8ec04ba764bee8602))

## [2.5.3](https://github.com/JNZader/apigen/compare/v2.5.2...v2.5.3) (2026-01-23)

### Bug Fixes

* **security:** remove @Component from SecurityProperties to avoid duplicate bean ([cfbab02](https://github.com/JNZader/apigen/commit/cfbab028450e15b001a98a2e56061ce85c41a99c))

## [2.5.2](https://github.com/JNZader/apigen/compare/v2.5.1...v2.5.2) (2026-01-23)

### Bug Fixes

* **security:** remove @ConditionalOnBean for Spring Framework 7.0 compatibility ([39befa2](https://github.com/JNZader/apigen/commit/39befa21f2652db2e7b37c0deff14675ff711e4e))

## [2.5.1](https://github.com/JNZader/apigen/compare/v2.5.0...v2.5.1) (2026-01-22)

### Bug Fixes

* **codegen:** use correct Duration format for cache expire-after-write ([c1b7165](https://github.com/JNZader/apigen/commit/c1b716574c92d6ec4e96eb13554446565064cfc5))

## [2.5.0](https://github.com/JNZader/apigen/compare/v2.4.2...v2.5.0) (2026-01-22)

### Features

* **codegen:** generate full application.yml with CORS, cache, rate-limit, and security config ([2333dc0](https://github.com/JNZader/apigen/commit/2333dc0283cf0196e1a0726b34794b08f57ae26b))

## [2.4.2](https://github.com/JNZader/apigen/compare/v2.4.1...v2.4.2) (2026-01-22)

### Bug Fixes

* **ci:** add permissions for security scan and update CodeQL to v4 ([63105fa](https://github.com/JNZader/apigen/commit/63105fab7ffc6d759012ed92077dd8976f8ed718))
* **security:** add auto-configuration for disabled security mode ([5f5b378](https://github.com/JNZader/apigen/commit/5f5b378acd83922bb455409a9cfce1150e6f485d))
* **security:** replace JWT example to avoid false positive in Trivy scan ([8ba1d26](https://github.com/JNZader/apigen/commit/8ba1d26ba3b2d8613429a4cd107c9f5f8c141e93))

## [2.4.1](https://github.com/JNZader/apigen/compare/v2.4.0...v2.4.1) (2026-01-22)

### Bug Fixes

* **ci:** add chmod +x gradlew to all test jobs ([a121cb7](https://github.com/JNZader/apigen/commit/a121cb7c49e59e7dcef24bf2850d58d7ca795b0a))
* **ci:** remove dorny/test-reporter due to permission issues in PRs ([ba84333](https://github.com/JNZader/apigen/commit/ba8433371cb5bed6c4ee49d857d93128a82a01e0))
* **codegen:** disable security in test profile when security module is enabled ([c44cac4](https://github.com/JNZader/apigen/commit/c44cac4d05c3395093c49c931f951e64b59fe798))

### CI/CD

* add granular test jobs per module ([db80ab7](https://github.com/JNZader/apigen/commit/db80ab708706217028f577326ed449924e687bab))

## [2.4.0](https://github.com/JNZader/apigen/compare/v2.3.0...v2.4.0) (2026-01-22)

### Features

* **server:** use stable JitPack versions for generated projects ([c2dcf03](https://github.com/JNZader/apigen/commit/c2dcf03a3f925129230354d937ac66e72d5c06bf))

## [2.3.0](https://github.com/JNZader/apigen/compare/v2.2.1...v2.3.0) (2026-01-22)

### Features

* **core:** add enabled flag to RateLimitingFilter for test profiles ([86c39e1](https://github.com/JNZader/apigen/commit/86c39e13531513fdc8f563a9d479e3b9b50c31ce))

## [2.2.1](https://github.com/JNZader/apigen/compare/v2.2.0...v2.2.1) (2026-01-22)

### Bug Fixes

* **codegen:** use correct property prefix for rate limit in test config ([2cfa075](https://github.com/JNZader/apigen/commit/2cfa0757e8e77540277d6e8a53b1747520ac5e6f))

## [2.2.0](https://github.com/JNZader/apigen/compare/v2.1.18...v2.2.0) (2026-01-22)

### Features

* **codegen:** generate application-test.yml with rate limiting disabled ([bdeef5d](https://github.com/JNZader/apigen/commit/bdeef5d95f530fecabe060b8a86d5605b198a7b9))

## [2.1.18](https://github.com/JNZader/apigen/compare/v2.1.17...v2.1.18) (2026-01-22)

### Bug Fixes

* **codegen:** use UUID for truly unique test values ([31aa15f](https://github.com/JNZader/apigen/commit/31aa15f255afa0049dc1dd1eecd6e0cca0a582c4))

## [2.1.17](https://github.com/JNZader/apigen/compare/v2.1.16...v2.1.17) (2026-01-22)

### Bug Fixes

* **codegen:** generate unique DTOs in cursor pagination test ([56b4888](https://github.com/JNZader/apigen/commit/56b4888ab5813d49c262a8df03be04d2d155d3da))
* **core:** make cache expiration test more robust ([b68a13a](https://github.com/JNZader/apigen/commit/b68a13a8f217d750379065ada56c47936ca604b7))
* **test:** use asMap() to avoid resetting cache expiration timer ([e7e99b0](https://github.com/JNZader/apigen/commit/e7e99b017ac52bc1fc0eae19373f71a77608d019))

## [2.1.16](https://github.com/JNZader/apigen/compare/v2.1.15...v2.1.16) (2026-01-22)

### Bug Fixes

* **codegen:** use PageRequest in PageImpl to fix Jackson serialization ([8264d7f](https://github.com/JNZader/apigen/commit/8264d7fca43e3f0fd4990c6f691e048ec85b64d9))

## [2.1.15](https://github.com/JNZader/apigen/compare/v2.1.14...v2.1.15) (2026-01-22)

### Bug Fixes

* **server:** upgrade generated projects to Gradle 9.3.0 ([cb01db5](https://github.com/JNZader/apigen/commit/cb01db5955c47953156cd4862ce34fb1bf30628d))

## [2.1.13](https://github.com/JNZader/apigen/compare/v2.1.12...v2.1.13) (2026-01-22)

### Bug Fixes

* **codegen:** migrate test generators from Jackson 2 to Jackson 3 ([93609cd](https://github.com/JNZader/apigen/commit/93609cd9643345512bb2dc69ee3c1d7e8f0c1c72))

## [2.1.12](https://github.com/JNZader/apigen/compare/v2.1.11...v2.1.12) (2026-01-22)

### Bug Fixes

* **codegen:** fix ObjectMapper import and add audit field ignores ([#46](https://github.com/JNZader/apigen/issues/46)) ([56ddcd9](https://github.com/JNZader/apigen/commit/56ddcd9d7dd641573f84efc2cdb3b742e0cd532e))

## [2.1.11](https://github.com/JNZader/apigen/compare/v2.1.10...v2.1.11) (2026-01-22)

### Bug Fixes

* **codegen:** resolve MapStruct cycle error in mapper generation ([#45](https://github.com/JNZader/apigen/issues/45)) ([2ed69c8](https://github.com/JNZader/apigen/commit/2ed69c87902ef1ed09573c6b858b7a5eef910086))

## [2.1.10](https://github.com/JNZader/apigen/compare/v2.1.9...v2.1.10) (2026-01-22)

### Bug Fixes

* **codegen:** use @InheritConfiguration instead of @SuperBuilder ([#44](https://github.com/JNZader/apigen/issues/44)) ([0510dcf](https://github.com/JNZader/apigen/commit/0510dcfc2704fe724e09d7dbeac2054241319c65))

## [2.1.9](https://github.com/JNZader/apigen/compare/v2.1.8...v2.1.9) (2026-01-22)

### Bug Fixes

* **codegen:** use @SuperBuilder for proper inheritance support ([#43](https://github.com/JNZader/apigen/issues/43)) ([76a5ab4](https://github.com/JNZader/apigen/commit/76a5ab41b257d555a8b013f4613e9209a8e682c1))

## [2.1.8](https://github.com/JNZader/apigen/compare/v2.1.7...v2.1.8) (2026-01-21)

### Bug Fixes

* **codegen:** resolve generated test failures and MapStruct warnings ([#42](https://github.com/JNZader/apigen/issues/42)) ([eee6d32](https://github.com/JNZader/apigen/commit/eee6d32e1d2191a6cbb8fe6689dcdb6853b13be4))

## [2.1.7](https://github.com/JNZader/apigen/compare/v2.1.6...v2.1.7) (2026-01-21)

### Bug Fixes

* **codegen:** resolve MapStruct warnings and Mockito 5 compatibility ([#41](https://github.com/JNZader/apigen/issues/41)) ([1f963a8](https://github.com/JNZader/apigen/commit/1f963a898a1f1eefc51baea2603b855c80df3595))

## [2.1.6](https://github.com/JNZader/apigen/compare/v2.1.5...v2.1.6) (2026-01-21)

### Bug Fixes

* **codegen:** fix test generators for Spring Boot 4 ([#40](https://github.com/JNZader/apigen/issues/40)) ([6b5b96a](https://github.com/JNZader/apigen/commit/6b5b96a2fe3b01866b3375900a76d756708e0494))

## [2.1.5](https://github.com/JNZader/apigen/compare/v2.1.4...v2.1.5) (2026-01-21)

### Bug Fixes

* **codegen:** correct JSON escaping in IntegrationTestGenerator ([#39](https://github.com/JNZader/apigen/issues/39)) ([da02415](https://github.com/JNZader/apigen/commit/da024157fbbb84b257f606540eeb467ab8520653))

## [2.1.4](https://github.com/JNZader/apigen/compare/v2.1.3...v2.1.4) (2026-01-21)

### Bug Fixes

* **codegen:** correct test generators for Spring Boot 4 compatibility ([#38](https://github.com/JNZader/apigen/issues/38)) ([15bcd62](https://github.com/JNZader/apigen/commit/15bcd62546649332394e6f13c1345b8ef4d181ab))

## [2.1.3](https://github.com/JNZader/apigen/compare/v2.1.2...v2.1.3) (2026-01-21)

### Bug Fixes

* **codegen:** use fully qualified @Order annotation ([#37](https://github.com/JNZader/apigen/issues/37)) ([93cb2d1](https://github.com/JNZader/apigen/commit/93cb2d173fae36de8bc354ac7adee2728da83ecd))

## [2.1.2](https://github.com/JNZader/apigen/compare/v2.1.1...v2.1.2) (2026-01-21)

### Bug Fixes

* **codegen:** update test generation for Spring Boot 4 compatibility ([#35](https://github.com/JNZader/apigen/issues/35)) ([4cd2470](https://github.com/JNZader/apigen/commit/4cd2470f9a354e9d97da4f2443e62f84b392db30))

## [2.1.1](https://github.com/JNZader/apigen/compare/v2.1.0...v2.1.1) (2026-01-21)

### Performance Improvements

* **db:** Phase 16 - Database Optimization ([#34](https://github.com/JNZader/apigen/issues/34)) ([496ce55](https://github.com/JNZader/apigen/commit/496ce55fa785ab2dfce5972205e3b4158eb0c4fd))

## [2.1.0](https://github.com/JNZader/apigen/compare/v2.0.0...v2.1.0) (2026-01-21)

### Features

* Phase 15 - API Enhancement ([#33](https://github.com/JNZader/apigen/issues/33)) ([4a39cb6](https://github.com/JNZader/apigen/commit/4a39cb6af41ad4a163ada1e8fc29d005aef062b0))

## [2.0.0](https://github.com/JNZader/apigen/compare/v1.1.1...v2.0.0) (2026-01-21)

### ⚠ BREAKING CHANGES

* Error messages and exception messages are now in English.
Applications using i18n should rely on the i18n module for localization.

Files updated:
- apigen-core: ApiError, ProblemDetail, FilterSpecificationBuilder,
  BaseServiceImpl, SSE components, config classes
- apigen-security: AuthService, JwtService, TokenBlacklistService,
  AccountLockoutService, CustomUserDetailsService, SecurityConfig
- Updated corresponding test files to expect English messages

### Features

* Phase 12 - Code Quality Improvements ([#31](https://github.com/JNZader/apigen/issues/31)) ([c609072](https://github.com/JNZader/apigen/commit/c60907288dc8e5274f4dae6ac6be0cf1685690ee))

## [1.1.1](https://github.com/JNZader/apigen/compare/v1.1.0...v1.1.1) (2026-01-21)

### Performance Improvements

* Phase 11 - Performance Optimization ([#30](https://github.com/JNZader/apigen/issues/30)) ([9e3ae30](https://github.com/JNZader/apigen/commit/9e3ae30715fbf801f8ad44f95a8a52304106250e))

## [1.1.0](https://github.com/JNZader/apigen/compare/v1.0.2...v1.1.0) (2026-01-20)

### Features

* **security:** Phase 10 Security Hardening ([#29](https://github.com/JNZader/apigen/issues/29)) ([244532d](https://github.com/JNZader/apigen/commit/244532d2393238f57b762d903790122ae9357d63))

## [1.0.2](https://github.com/JNZader/apigen/compare/v1.0.1...v1.0.2) (2026-01-20)

### Bug Fixes

* **docker:** add missing modules to Dockerfile ([adfa328](https://github.com/JNZader/apigen/commit/adfa3281dae23bb468a214616a6eaf1e770055ab))

### Documentation

* switch from Docusaurus to Docsify for simpler documentation ([e0f0d6c](https://github.com/JNZader/apigen/commit/e0f0d6cf4cad71afe02c1ec3cfbc0d7036de4208))

## [1.0.1](https://github.com/JNZader/apigen/compare/v1.0.0...v1.0.1) (2026-01-20)

### Bug Fixes

* **docs:** remove non-existent doc references from sidebar ([aaef87a](https://github.com/JNZader/apigen/commit/aaef87a5e275978695e6f86ae4cfc4f10cdc6763))
* **docs:** simplify to use only Redocusaurus for API reference ([2d1523b](https://github.com/JNZader/apigen/commit/2d1523b024063157d9230e3c809d5dd0f12070f8))

### Documentation

* add Docusaurus documentation site and JitPack configuration ([d3f3153](https://github.com/JNZader/apigen/commit/d3f3153bcf7eb137e29eec7844b1fc969fc700e4))

## 1.0.0 (2026-01-20)

### Features

* add code quality tools (Spotless, Errorprone, JaCoCo, ArchUnit) ([3b231d3](https://github.com/JNZader/apigen/commit/3b231d33207037efed36fae4a3082041ff3e8771))
* **core:** Phase 6 - Additional Features ([#21](https://github.com/JNZader/apigen/issues/21)) ([2930355](https://github.com/JNZader/apigen/commit/2930355346d0308deaad073dccb07295272b8749))
* Phase 3 - Advanced Testing (PIT + JMH) ([#18](https://github.com/JNZader/apigen/issues/18)) ([074297e](https://github.com/JNZader/apigen/commit/074297e6aafd3936f319fbb55a115aa65090de0b))
* Phase 4 - Performance & Feature Flags ([#19](https://github.com/JNZader/apigen/issues/19)) ([7f0469b](https://github.com/JNZader/apigen/commit/7f0469b3d8424878f9d5952f06f2b4ea2edf8ec9))
* Phase 7 - Advanced Architecture ([#27](https://github.com/JNZader/apigen/issues/27)) ([5394cec](https://github.com/JNZader/apigen/commit/5394ceccb069e73a95a9fec46a9f16e67e0606ac))
* Phase 8 - Release & Deploy ([#28](https://github.com/JNZader/apigen/issues/28)) ([f1a42de](https://github.com/JNZader/apigen/commit/f1a42de65c0a3cb3f2d8def25ab3372f62dcb5e1))
* **security:** add configurable headers and Bucket4j rate limiting ([6daac47](https://github.com/JNZader/apigen/commit/6daac475ef9c8bc0f24d298f7607e28f8e70eead))
* **security:** add JWT key rotation support ([50cc0a7](https://github.com/JNZader/apigen/commit/50cc0a7ff803044adbab322faa4366bd82b46a71))

### Bug Fixes

* **ci:** convert image name to lowercase for registry ([1fa10c8](https://github.com/JNZader/apigen/commit/1fa10c8236d4a2c4047ef847b82116239be97fd6))
* **ci:** optimize Docker build and fix attestation ([aeb462e](https://github.com/JNZader/apigen/commit/aeb462e303dc2bcec62a30c4ce11df1af5475273))
* **docker:** update Dockerfile for multi-module project structure ([fc934eb](https://github.com/JNZader/apigen/commit/fc934eb6a2697a0578007e4436abedbd64c0d155))
* **gateway:** update to Spring Cloud 2025.1.0 artifact names ([1ae546a](https://github.com/JNZader/apigen/commit/1ae546ab80106ea07ddf837ae732372db704a1bb))
* **test:** fix CI test failures ([7564546](https://github.com/JNZader/apigen/commit/756454638dff42bf72dc00a9fae9078d62073bd8))

### Documentation

* add CHANGELOG.md and update gitignore ([6069add](https://github.com/JNZader/apigen/commit/6069add24d8c8b7a4de6dc6bb012dcf537b50c67))
* Phase 5 - Documentation improvements ([#20](https://github.com/JNZader/apigen/issues/20)) ([e48bcb1](https://github.com/JNZader/apigen/commit/e48bcb1f376161286d2e72588b49fcc4b62e5a67))
* update CHANGELOG and ROADMAP with Phase 0-2 completion ([bb48f07](https://github.com/JNZader/apigen/commit/bb48f07539a599f948fcea089eec1aac54a9b72c))
* update CHANGELOG with Spring Cloud 2025.1.0 artifact name fix ([b25268c](https://github.com/JNZader/apigen/commit/b25268c768fe44d7b6a0928386e26e2fd9e4e469))

### Build System

* **deps:** bump ch.qos.logback:logback-classic from 1.5.16 to 1.5.25 ([#17](https://github.com/JNZader/apigen/issues/17)) ([1b7bb5b](https://github.com/JNZader/apigen/commit/1b7bb5bfb1d61c5496a701d6d7203fbb40deb025))
* **deps:** bump com.diffplug.spotless from 7.0.2 to 8.1.0 ([#11](https://github.com/JNZader/apigen/issues/11)) ([a5237c6](https://github.com/JNZader/apigen/commit/a5237c6e92db5417788b664a1673b4482e4d8148))
* **deps:** bump com.google.errorprone:error_prone_core ([#22](https://github.com/JNZader/apigen/issues/22)) ([0b36b87](https://github.com/JNZader/apigen/commit/0b36b872a8fd4dd4ea4cf4240e570512c726723f))
* **deps:** bump com.tngtech.archunit:archunit-junit5 from 1.4.0 to 1.4.1 ([#24](https://github.com/JNZader/apigen/issues/24)) ([7ac47d0](https://github.com/JNZader/apigen/commit/7ac47d0445d9c6583e000f6f825e062e078cc668))
* **deps:** bump io.micrometer:micrometer-tracing-bridge-otel ([4ab4d18](https://github.com/JNZader/apigen/commit/4ab4d1846bf767427458449fe7178a798c88edc1))
* **deps:** bump net.ltgt.errorprone from 4.1.0 to 4.4.0 ([#16](https://github.com/JNZader/apigen/issues/16)) ([75de3ec](https://github.com/JNZader/apigen/commit/75de3ec7830dcb0a0b384cc90a8df632ea57d457))
* **deps:** bump org.mockito:mockito-junit-jupiter ([#26](https://github.com/JNZader/apigen/issues/26)) ([68b25c1](https://github.com/JNZader/apigen/commit/68b25c18e4d55a1ea97db04c511878139a379349))
* **deps:** bump org.slf4j:slf4j-api from 2.0.16 to 2.0.17 ([#12](https://github.com/JNZader/apigen/issues/12)) ([8dbd57b](https://github.com/JNZader/apigen/commit/8dbd57b95a636b2dda95cf90515542d4b3bd60a4))
* **deps:** bump org.sonarqube from 6.3.1.5724 to 7.2.2.6593 ([b8316de](https://github.com/JNZader/apigen/commit/b8316de6b17e9d3a06013a0564af4bbf365e0265))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([09fe95e](https://github.com/JNZader/apigen/commit/09fe95ed04264410042121927b4810aee4907873))
* **deps:** bump org.springframework.boot from 4.0.0 to 4.0.1 ([b4fa7c0](https://github.com/JNZader/apigen/commit/b4fa7c012079ef8b8fb2e0230912fb30dbc3588e))
* **deps:** bump org.springframework.cloud:spring-cloud-dependencies ([#25](https://github.com/JNZader/apigen/issues/25)) ([4b920f5](https://github.com/JNZader/apigen/commit/4b920f585f2f6f17fca7fe3db83ffe163fc96a58))
* **deps:** bump org.testcontainers:testcontainers-junit-jupiter ([daf8a3f](https://github.com/JNZader/apigen/commit/daf8a3f5231d5ec594860dca38d750f5c30c651d))
* **deps:** bump testcontainersVersion from 1.20.4 to 1.21.4 ([#14](https://github.com/JNZader/apigen/issues/14)) ([5b77870](https://github.com/JNZader/apigen/commit/5b778708704dff149edf80721bcc1c7ed60e6932))

### CI/CD

* **deps:** bump actions/attest-build-provenance from 1 to 3 ([#13](https://github.com/JNZader/apigen/issues/13)) ([a557623](https://github.com/JNZader/apigen/commit/a5576233024491b17681972b12dac95a22e11336))
* **deps:** bump actions/checkout from 4 to 6 ([c2ee47d](https://github.com/JNZader/apigen/commit/c2ee47dadf5cf23cfbef4d1317da1038a6bc4534))
* **deps:** bump actions/setup-java from 4 to 5 ([c5b5e89](https://github.com/JNZader/apigen/commit/c5b5e89c19aa3ca371b8f22c42de4ad3a11dd78f))
* **deps:** bump actions/upload-artifact from 4 to 6 ([90a9c8c](https://github.com/JNZader/apigen/commit/90a9c8c3fdbc631a5157a9b3db1fe27c3a23bab0))
* **deps:** bump github/codeql-action from 3 to 4 ([#15](https://github.com/JNZader/apigen/issues/15)) ([34c0df0](https://github.com/JNZader/apigen/commit/34c0df03bfd5418e20dfb01d2ded0f63fae8fc0a))
* **deps:** bump gradle/actions from 4 to 5 ([2b270e1](https://github.com/JNZader/apigen/commit/2b270e107d08f8731ed4016e66eeca47edf6d88a))

## [Unreleased]

### Added

#### Fase 0: Fundamentos DevOps
- ROADMAP.md with improvement plan
- CHANGELOG.md following Keep a Changelog convention
- README.md for each module (bom, core, security, codegen, server)
- LICENSE file (MIT)

#### Fase 1: Calidad de Código
- Spotless plugin (google-java-format) for consistent formatting
- Error Prone static analysis for compile-time bug detection
- JaCoCo code coverage with 70% minimum threshold
- ArchUnit tests for architecture validation (hexagonal, naming conventions)
- Pre-commit hooks via Gradle task for validation before commits

#### Fase 2: Seguridad Básica
- Configurable security headers (CSP, HSTS, Referrer-Policy, Permissions-Policy)
- Rate limiting with Bucket4j 8.16.0 (in-memory + Redis distributed)
- JWT key rotation support with `kid` header for zero-downtime rotation
- Multiple signing keys registry for transition periods

#### Fase 3: Testing Avanzado
- PIT mutation testing with Gradle plugin 1.19.0-rc.2
- JUnit 5 integration (pitest 1.22.0, junit5-plugin 1.2.1)
- Mutation threshold 40%, coverage threshold 50%
- `pitestAll` aggregate task for all modules
- Results: core 68%, security 60%, codegen 67% mutation coverage
- JMH benchmark infrastructure with plugin v0.7.3
- ResultBenchmark demonstrating ~1M+ ops/ms for core operations
- `jmhAll` aggregate task for all modules
- **Contract Testing (3.2)**: Spring Cloud Contract consumer-driven contract tests
  - Spring Cloud Contract Verifier 5.0.1 (Gradle plugin)
  - Spring Cloud Dependencies 2025.1.0 BOM for Spring Boot 4.0 compatibility
  - REST-Assured spring-mock-mvc 5.5.7 (Spring Framework 7 compatible)
  - Groovy DSL contracts for REST endpoint validation
  - Contracts for pagination (`findAll`), count headers, error responses (400, 404, 412)
  - RFC 7807 Problem Detail format validation (`application/problem+json`)
  - Optimistic concurrency control testing (If-Match/ETag headers)
  - `BaseContractTest` base class with test entity setup
  - `contractTest` source set and Gradle task integration
  - 6 contract tests covering REST API compliance

#### Fase 4: Rendimiento + Feature Flags
- Togglz feature flags with manual configuration (Spring Boot 4 compatible)
- Redis distributed cache support (RedisCacheConfig) as alternative to Caffeine
- HikariCP metrics exposed to Prometheus (HikariMetricsConfig)
- N+1 query detection via Hibernate Statistics (QueryAnalysisConfig)
- Async batch operations with Virtual Threads (BatchService)
- FeatureChecker utility for runtime feature state checks
- QueryAssertions for test query count validation

#### Fase 5: Documentación
- OpenAPI examples with @Schema annotations on all DTOs (security, example modules)
- Enhanced OpenApiConfig with security schemes and error response schemas
- RFC 7807 Problem Detail examples for all error types (400, 401, 403, 404, 409, 412, 500)
- C4 architecture diagrams (Context, Container, Component levels) in Mermaid format
- Sequence diagrams for Create Resource and Authentication flows
- Updated FEATURES.md with Phase 4 features documentation (sections 17-21)
- Added docs/architecture folder with C4_ARCHITECTURE.md

#### Fase 6: Features Adicionales (Partial)
- **Internationalization (i18n)**: Full i18n support for error messages
  - `MessageService` for retrieving localized messages
  - `I18nConfig` with Accept-Language header locale resolution
  - Support for English (default) and Spanish locales
  - Message bundles: `messages.properties`, `messages_es.properties`
  - LocaleChangeInterceptor for `?lang=` query parameter
  - GlobalExceptionHandler integration for localized RFC 7807 responses
  - Comprehensive test coverage for both locales
- **Webhooks System**: Event-driven webhook notifications
  - `WebhookEvent` enum with 13 event types (entity lifecycle, batch, user, security, system)
  - `WebhookPayload` record with builder pattern for event data
  - `WebhookSubscription` record for managing webhook endpoints
  - `WebhookDelivery` record for tracking delivery status (SUCCESS, FAILED_WILL_RETRY, FAILED_PERMANENT)
  - `WebhookSignature` utility with HMAC-SHA256 signatures for request authentication
  - `WebhookService` with async delivery using virtual threads
  - Configurable retry logic with exponential backoff (base delay, max delay, max retries)
  - `WebhookSubscriptionRepository` interface with `InMemoryWebhookSubscriptionRepository` implementation
  - `WebhookAutoConfiguration` for Spring Boot auto-configuration (`apigen.webhooks.enabled=true`)
  - Ping functionality for testing webhook endpoints
  - Delivery callback support for custom delivery handling
  - Comprehensive test suite (79 tests) covering dispatch, retry logic, signatures, and callbacks
- **Bulk Import/Export**: CSV and Excel support for mass data operations
  - `BulkFormat` enum supporting CSV and EXCEL (XLSX) formats
  - Format detection from filename and content-type
  - `BulkOperationResult` record with operation statistics (success/failure counts, duration)
  - `RecordError` for detailed error tracking (row number, field name, raw value)
  - `BulkImportService` interface with configurable import options
    - `ImportConfig`: skipHeader, stopOnError, batchSize, csvSeparator, dateFormat, sheetName
    - Support for custom processing functions with error handling
    - Validation mode for dry-run imports
  - `BulkExportService` interface with flexible export options
    - `ExportConfig`: includeHeader, csvSeparator, dateFormat, sheetName, autoSizeColumns
    - Field inclusion/exclusion support
    - Stream-based export for large datasets
  - `BulkOperationsService` implementation
    - CSV parsing with OpenCSV 5.9 (CsvToBean/StatefulBeanToCsv)
    - Excel export with Apache POI 5.3.0 (SXSSFWorkbook streaming for memory efficiency)
    - Support for both Record types and POJOs via reflection
    - Automatic header extraction from @CsvBindByName annotations
  - `BulkAutoConfiguration` for Spring Boot auto-configuration (`apigen.bulk.enabled=true`)
  - Comprehensive test suite (26 tests) covering import, export, configurations

#### Fase 8: Release & Deploy
- **Semantic-release (8.1)**: Automated versioning and release workflow
  - GitHub Actions workflow `.github/workflows/release.yml`
  - Conventional Commits parsing for automatic version bumps
  - Automatic CHANGELOG generation on release
  - GitHub Releases with release notes
- **GraalVM Native Image (8.2)**: Ahead-of-time compilation support
  - `org.graalvm.buildtools.native` plugin 0.10.6 in apigen-example
  - Spring AOT processing enabled
  - Native image build configuration with URL protocols, charsets
  - GraalVM JDK 25 toolchain support
- **PKCE OAuth2 (8.3)**: Enhanced OAuth2 security for public clients
  - `PKCEService` implementing RFC 7636 (Proof Key for Code Exchange)
  - Code verifier generation (43-128 chars, URL-safe)
  - S256 (SHA-256) and plain code challenge methods
  - `PKCEAuthorizationStore` for authorization code lifecycle (single-use, expiration)
  - `OAuth2Controller` with `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke` endpoints
  - `PKCETokenRequestDTO` supporting authorization_code and refresh_token grants
  - PKCE helper endpoint `/oauth2/pkce/generate` for development
  - `PkceProperties` configuration class for customization
  - Comprehensive test suite (21 PKCEService tests, store tests)

#### Fase 7: Arquitectura Avanzada (Partial)
- **API Versioning (7.6)**: Complete API versioning infrastructure
  - `@ApiVersion` annotation for marking API versions on controllers/methods
  - `@DeprecatedVersion` annotation with RFC 8594 support (since, sunset, successor, migrationGuide)
  - `VersioningStrategy` enum: PATH, HEADER, QUERY_PARAM, MEDIA_TYPE
  - `ApiVersionResolver` with builder pattern for configuring resolution strategies
  - `ApiVersionInterceptor` for automatic deprecation headers (Deprecation, Sunset, Link)
  - `VersionContext` thread-local holder for current API version with comparison utilities
  - `ApiVersionAutoConfiguration` for Spring Boot (`apigen.versioning.enabled=true`)
  - Comprehensive test suite (45 tests) covering all resolution strategies and deprecation headers
- **Multi-tenancy (7.1)**: Native SaaS multi-tenancy support
  - `TenantContext` using InheritableThreadLocal for tenant propagation
  - `TenantResolver` with multiple resolution strategies (HEADER, SUBDOMAIN, PATH, JWT_CLAIM)
  - `TenantResolutionStrategy` enum for configurable tenant identification
  - `TenantFilter` servlet filter with excluded paths support and tenant validation
  - `TenantAware` interface for tenant-aware entities
  - `TenantEntityListener` JPA listener for automatic tenant assignment on @PrePersist/@PreUpdate
  - `TenantMismatchException` for cross-tenant access attempts
  - `TenantAutoConfiguration` for Spring Boot (`apigen.multitenancy.enabled=true`)
  - Tenant ID validation with configurable patterns
  - Custom header name support (default: X-Tenant-ID)
  - Comprehensive test suite covering all resolution strategies and filter behavior
- **Event Sourcing (7.2)**: Event sourcing infrastructure for aggregates
  - `DomainEvent` interface for all domain events with metadata support
  - `StoredEvent` JPA entity for persisting events with indexes
  - `EventStore` interface for append-only event storage with optimistic concurrency
  - `JpaEventStore` JPA implementation with Spring event publishing
  - `Snapshot` JPA entity for aggregate state snapshots
  - `EventSourcedAggregate` base class for event-sourced aggregates
  - `AggregateRepository` for loading/saving aggregates with snapshot support
  - `EventSerializer` JSON serializer with Jackson and Java Time support
  - `ConcurrencyException` for optimistic locking conflicts
  - `EventSourcingAutoConfiguration` for Spring Boot (`apigen.eventsourcing.enabled=true`)
  - Comprehensive test suite (37 tests) covering serialization, aggregates, and event store
- **GraphQL Module (7.3)**: New `apigen-graphql` module for GraphQL API layer
  - `SchemaBuilder` fluent API for constructing GraphQL schemas programmatically
  - `GraphQLExecutor` for executing queries, mutations, and subscriptions
  - `GraphQLContext` request-scoped context with user ID, locale, and custom attributes
  - `BaseDataFetcher` base class for data fetchers with utility methods
  - `DataLoaderRegistry` for N+1 query prevention with batched loading
  - `DataLoaderRegistrar` interface for registering DataLoaders
  - `GraphQLExceptionHandler` for RFC 7807-aligned error responses
  - `ApiGenGraphQLError` custom error with type, status code, and extensions
  - `GraphQLErrorType` enum for semantic error classification
  - `GraphQLRequest` record for HTTP request parsing
  - `GraphQLController` HTTP endpoint at `/graphql`
  - `GraphQLAutoConfiguration` for Spring Boot (`apigen.graphql.enabled=true`)
  - GraphQL Java 22.3 and Java DataLoader 3.4.0 integration
  - Comprehensive test suite covering schema building, execution, errors, and data loading
- **gRPC Module (7.4)**: New `apigen-grpc` module for inter-service communication
  - `GrpcServer` wrapper with fluent builder API for server lifecycle management
  - `GrpcChannelFactory` for creating and managing client channels with caching
  - `LoggingServerInterceptor` / `LoggingClientInterceptor` for call logging with timing
  - `ExceptionHandlingInterceptor` mapping exceptions to gRPC status codes
  - `AuthenticationServerInterceptor` for token-based authentication with excluded methods
  - `AuthenticationClientInterceptor` for adding Bearer tokens to outgoing requests
  - `HealthServiceManager` for aggregating health checks from multiple components
  - `HealthCheck` interface with Result record for health status reporting
  - Proto definitions: common.proto (Timestamp, PageRequest, OperationResult, ErrorDetail, EntityId, AuditInfo)
  - Proto definitions: health.proto (HealthService with Check and Watch RPCs)
  - `GrpcAutoConfiguration` for Spring Boot (`apigen.grpc.enabled=true`)
  - gRPC Java 1.72.0, Protobuf 4.31.1, gRPC Spring Boot Starter 3.1.0.RELEASE
  - Comprehensive test suite (37 tests) covering server, client, interceptors, and health checks
- **API Gateway Module (7.5)**: New `apigen-gateway` module for API Gateway functionality
  - `LoggingGatewayFilter` global filter with correlation ID generation and request/response logging
  - `AuthenticationGatewayFilter` JWT-based authentication filter with configurable paths
  - `AuthResult` record for authentication results (success/failure with user details)
  - `RateLimitKeyResolver` with multiple strategies (IP, USER_ID, API_KEY, COMPOSITE, PATH)
  - Path normalization for rate limiting (replaces numeric IDs and UUIDs with placeholders)
  - `CircuitBreakerGatewayFilter` with configurable timeout and custom fallback support
  - `RequestTimingGatewayFilter` for metrics collection (request duration, status codes)
  - `RouteBuilder` fluent API for programmatic route definition
  - `RouteDefinition` record with predicates, filters, circuit breaker, timeout, metadata
  - `DynamicRouteLocator` for runtime route management (add/remove/update routes)
  - `GatewayProperties` configuration with nested classes for rate limiting, circuit breaker, auth, CORS
  - `GatewayAutoConfiguration` for Spring Boot (`apigen.gateway.enabled=true`)
  - Spring Cloud Gateway 2025.1.0 (spring-cloud-gateway-server-webflux), Resilience4j reactor integration
  - Comprehensive test suite (63 tests) covering filters, rate limiting, circuit breaker, routes

### Changed
- Updated .gitignore to exclude logs, .env, and .claude files
- Optimized Docker CI (single platform for main, multi-platform for releases)
- Converted image name to lowercase for registry compatibility
- Updated OpenTelemetry from 1.53.0 to 1.58.0
- Updated JSQLParser from 5.0 to 5.3

### Security
- Added X-RateLimit-* headers for API throttling visibility
- Added Retry-After header on 429 responses
- Stricter rate limits on authentication endpoints (10 req/min vs 100 req/s)
- Fixed CVE-2020-8908, CVE-2023-2976: Upgraded Guava from 30.1-jre to 33.5.0-jre (insecure temp directory)
- Fixed Docker image CVEs: Added `apk upgrade` for libpng, libtasn1, BusyBox vulnerabilities

### Fixed
- Updated apigen-gateway to use `spring-cloud-gateway-server-webflux` (replaces deprecated `spring-cloud-starter-gateway` in Spring Cloud 2025.1.0)
- Excluded eventsourcing infrastructure package from domain event architecture test (StoredEvent is infrastructure, not domain)

---

## [1.0.0-SNAPSHOT] - 2025-01-18

### Added

#### Core Module (apigen-core)
- `Base` abstract entity with soft delete, auditing, optimistic locking
- `BaseService` / `BaseServiceImpl` with caching and Result pattern
- `BaseController` / `BaseControllerImpl` with HATEOAS support
- `BaseRepository` with soft delete filtering
- `BaseMapper` interface for MapStruct
- `BaseResourceAssembler` for HATEOAS links
- Dynamic filtering with 12+ operators (eq, like, gte, between, etc.)
- Cursor-based pagination for large datasets
- ETag support for conditional requests
- Domain events (Created, Updated, Deleted, Restored)
- Virtual threads async configuration
- Caffeine caching with configurable TTL
- Resilience4j circuit breaker integration
- OpenTelemetry tracing support
- Spring Boot auto-configuration

#### Security Module (apigen-security)
- JWT authentication with HS512 algorithm
- Access token + Refresh token flow
- Token blacklisting for logout/revocation
- User, Role, Permission entities
- Rate limiting on authentication endpoints
- Security audit logging
- Spring Security auto-configuration

#### CodeGen Module (apigen-codegen)
- SQL schema parser (JSQLParser)
- Entity generator (JPA + Base)
- DTO generator (records + validation)
- Repository generator
- Service generator
- Controller generator
- Mapper generator (MapStruct)
- Migration generator (Flyway)
- Test generator

#### Server Module (apigen-server)
- HTTP endpoint for code generation
- ZIP file response with generated project

#### BOM Module (apigen-bom)
- Centralized dependency version management
- Spring Boot 4.0.0
- Spring Cloud 2024.0.1
- All third-party versions managed

#### Example Module (apigen-example)
- Product entity example
- Complete CRUD demonstration
- Flyway migrations
- Dev/Prod profiles

#### DevOps
- Multi-stage Dockerfile with Alpine JRE 25
- Docker Compose stack (PostgreSQL, Prometheus, Grafana)
- GitHub Actions CI pipeline
- Dependabot configuration
- Issue and PR templates

---

## Types of Changes

- `Added` for new features
- `Changed` for changes in existing functionality
- `Deprecated` for soon-to-be removed features
- `Removed` for now removed features
- `Fixed` for any bug fixes
- `Security` for vulnerability fixes
