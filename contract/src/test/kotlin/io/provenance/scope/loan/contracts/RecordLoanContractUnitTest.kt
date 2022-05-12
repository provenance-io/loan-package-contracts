package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.loan.test.Constructors.recordContractWithEmptyScope
import io.provenance.scope.loan.test.LoanPackageArbs.anyNonEmptyString
import io.provenance.scope.loan.test.LoanPackageArbs.anyUuid
import io.provenance.scope.loan.test.shouldBeParseFailureFor
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.UnexpectedContractStateException
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.Loan
import tech.figure.proto.util.toProtoAny
import tech.figure.util.v1beta1.UUID as FigureTechUUID

class RecordLoanContractUnitTest : WordSpec({
    "recordAsset" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                Asset.getDefaultInstance().let { emptyAssetWithoutLoan ->
                    shouldThrow<UnexpectedContractStateException> {
                        recordContractWithEmptyScope.recordAsset(emptyAssetWithoutLoan)
                    }.let { exception ->
                        exception.message shouldContainIgnoringCase "No key \"loan\" was found"
                    }
                }
            }
        }
        "given an input with a loan value of an incorrect type" should {
            "throw an appropriate exception" {
                Asset.newBuilder().also { assetBuilder ->
                    assetBuilder.putKv("loan", FigureTechUUID.getDefaultInstance().toProtoAny())
                }.build().let { assetWithBadLoanType ->
                    shouldThrow<UnexpectedContractStateException> {
                        recordContractWithEmptyScope.recordAsset(assetWithBadLoanType)
                    }.let { exception ->
                        exception shouldBeParseFailureFor "tech.figure.loan.v1beta1.Loan"
                    }
                }
            }
        }
        "given an input with invalid changes to existing data" should {
            "throw an appropriate exception" {
                val existingAsset = Asset.newBuilder().apply {
                    id = randomProtoUuid // To mark the existing asset as being set
                    putKv(
                        "loan",
                        Loan.newBuilder().also { loanBuilder ->
                            loanBuilder.id = randomProtoUuid
                        }.build().toProtoAny()
                    )
                }.build()
                val newAsset = Asset.newBuilder().also { assetBuilder ->
                    assetBuilder.putKv(
                        "loan",
                        Loan.newBuilder().also { loanBuilder ->
                            loanBuilder.id = randomProtoUuid
                        }.build().toProtoAny()
                    )
                }.build()
                shouldThrow<ContractViolationException> { // Assuming no UUID collision...
                    RecordLoanContract(
                        existingAsset = existingAsset,
                        existingENote = ENote.getDefaultInstance(),
                    ).recordAsset(newAsset)
                }.let { exception ->
                    exception.message shouldContainIgnoringCase "Cannot change asset ID"
                }
            }
        }
        "given an valid input with no existing asset record on scope" should {
            "not throw an exception" {
                checkAll(anyUuid, anyUuid, anyNonEmptyString, anyNonEmptyString) { randomAssetId, randomLoanId, randomType, randomOriginatorName ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv(
                                "loan",
                                Loan.newBuilder().also { loanBuilder ->
                                    loanBuilder.id = randomLoanId
                                    loanBuilder.originatorName = randomOriginatorName
                                }.build().toProtoAny()
                            )
                        }.build()
                    )
                }
            }
        }
        "given an valid input with an existing asset record on scope" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "recordServicingRights" When {
        "given an invalid input" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an valid input" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "recordDocuments" When {
        "given an invalid input" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an valid input" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "recordServicingData" When {
        "given an invalid input" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an valid input" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "recordValidationData" When {
        "given an invalid input" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an valid input" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "recordENote" When {
        "given an invalid input" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an valid input with no existing eNote record on scope" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given an valid input with an existing eNote record on scope" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
})
