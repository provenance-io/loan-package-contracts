# loan-package-contracts
Defines a loan package scope specification and [p8e](https://github.com/provenance-io/p8e-scope-sdk/)
smart contracts that can be executed against it.
## Status
[![stability-beta][stability-badge]][stability-info]
[![Code Coverage][code-coverage-badge]][code-coverage-report]
[![Latest Release][release-badge]][release-latest]

[![LOC][loc-badge]][loc-url]
### Artifacts
#### Contracts JAR
[![Contracts Artifact][contracts-publication-badge]][contracts-publication-url]
#### Protobuf JAR
[![Proto Artifact][proto-publication-badge]][proto-publication-url]

[stability-badge]: https://img.shields.io/badge/stability-beta-33bbff.svg?style=for-the-badge
[stability-info]: https://github.com/mkenney/software-guides/blob/master/STABILITY-BADGES.md#beta
[code-coverage-badge]: https://img.shields.io/codecov/c/gh/provenance-io/loan-package-contracts/main?label=Codecov&style=for-the-badge
[code-coverage-report]: https://app.codecov.io/gh/provenance-io/loan-package-contracts
[release-badge]: https://img.shields.io/github/v/tag/provenance-io/loan-package-contracts.svg?sort=semver&style=for-the-badge
[release-latest]: https://github.com/provenance-io/loan-package-contracts/releases/latest
[contracts-publication-badge]: https://maven-badges.herokuapp.com/maven-central/io.provenance.loan-package/contract/badge.svg?style=for-the-badge
[contracts-publication-url]: https://maven-badges.herokuapp.com/maven-central/io.provenance.loan-package/contract
[proto-publication-badge]: https://maven-badges.herokuapp.com/maven-central/io.provenance.loan-package/proto/badge.svg?style=for-the-badge
[proto-publication-url]: https://maven-badges.herokuapp.com/maven-central/io.provenance.loan-package/proto
[license-badge]: https://img.shields.io/github/license/provenance-io/loan-package-contracts.svg
[license-url]: https://github.com/provenance-io/loan-package-contracts/blob/main/LICENSE
[loc-badge]: https://tokei.rs/b1/github/provenance-io/loan-package-contracts
[loc-url]: https://github.com/provenance-io/loan-package-contracts
## Development
### Commands
#### Cloning the Repository
```shell
git clone https://github.com/provenance-io/loan-package-contracts.git
cd loan-package-contracts/
```
#### Building the project
```shell
./gradlew clean build --refresh-dependencies
```
#### Bootstrapping the contracts
See [here](https://github.com/provenance-io/p8e-gradle-plugin/#tasks) for more details on the p8e tasks.

The following command will put the contracts into an [object store](https://github.com/provenance-io/object-store): 
```shell
./gradlew p8eClean p8eCheck p8eBootstrap
```
In order for the bootstrapping to succeed, the following environment variables will need to be defined:
```shell
OS_GRPC_URL             # The URL to your object store that will store the contracts
PROVENANCE_GRPC_URL     # The URL to the Provenance instance the contracts will run against
ENCRYPTION_PRIVATE_KEY
SIGNING_PRIVATE_KEY
CHAIN_ID                # The ID of the chain - use chain-local locally and pio-testnet-1 for the test environment
```
For local development, you can quickly get necessary Docker containers running and said environment values populated by
using [p8e-scope-sdk/dev-tools](https://github.com/provenance-io/p8e-scope-sdk/blob/main/dev-tools/compose/README.md):
```shell
git clone https://github.com/provenance-io/p8e-scope-sdk.git
cd p8e-scope-sdk/dev-tools/compose/
docker compose up -d
source ./host.env
cd /PathToYour/loan-package-contracts
./gradlew p8eBootstrap
```
#### Publishing local artifacts
To publish to your machine's local Maven repository, use
```shell
./gradlew publishToMavenLocal -xsignMavenPublication
```
#### Linting
This project uses [Ktlint](https://github.com/pinterest/ktlint).

To immediately view linting errors, use
```shell
./gradlew clean ktlintCheck
```
To generate linting reports, use
```shell
./gradlew ktlintCheck
```
To have the linter try to update your code to fit the linting rules, use
```shell
./gradlew ktlintFormat
```
