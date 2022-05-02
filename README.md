# loan-package-contracts
Defines a loan package scope definition and p8e smart contracts that can be executed against it
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
```shell
./gradlew p8eClean p8eCheck p8eBootstrap
```
See [here](https://github.com/provenance-io/p8e-gradle-plugin/#tasks) for details on the p8e tasks.
#### Linting
This project uses [Ktlint](https://github.com/pinterest/ktlint).

To run the linter against your code, use
```shell
./gradlew ktlintCheck
```
To have the linter update your code to fit the linting rules, use
```shell
./gradlew ktlintFormat
```
