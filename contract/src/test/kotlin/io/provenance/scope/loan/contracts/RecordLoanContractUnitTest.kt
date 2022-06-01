package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.checkAll
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.LoanScopeProperties.assetMismoKey
import io.provenance.scope.loan.test.Constructors.recordContractWithEmptyScope
import io.provenance.scope.loan.test.LoanPackageArbs.anyInvalidUuid
import io.provenance.scope.loan.test.LoanPackageArbs.anyNonEmptyString
import io.provenance.scope.loan.test.LoanPackageArbs.anyNonUliString
import io.provenance.scope.loan.test.LoanPackageArbs.anyUuid
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidFigureTechLoan
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidMismoLoan
import io.provenance.scope.loan.test.LoanPackageArbs.anyValidUli
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.proto.util.toProtoAny
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.loan.v1beta1.Loan as FigureTechLoan
import tech.figure.util.v1beta1.UUID as FigureTechUUID

private val FigureTechLoanIdentifier = FigureTechLoan::class.java.toString()
private val MISMOLoanIdentifier = MISMOLoanMetadata::class.java.toString()

class RecordLoanContractUnitTest : WordSpec({
    "recordAsset" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                Asset.getDefaultInstance().let { emptyAssetWithoutLoan ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordAsset(emptyAssetWithoutLoan)
                    }.let { exception ->
                        exception.message shouldContainIgnoringCase "Asset must have valid ID"
                        exception.message shouldContainIgnoringCase "Asset is missing type"
                        exception.message shouldContainIgnoringCase
                            "Exactly one of \"$assetLoanKey\" or \"$assetMismoKey\" must be a key in the input asset"
                    }
                }
            }
        }
        "given an input to an empty scope with a loan value not of the expected Figure Tech loan type" should {
            "throw an appropriate exception" {
                Asset.newBuilder().also { assetBuilder ->
                    assetBuilder.putKv("loan", FigureTechUUID.getDefaultInstance().toProtoAny())
                }.build().let { assetWithBadLoanType ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordAsset(assetWithBadLoanType)
                    }.let { exception ->
                        exception.message shouldContain "Could not unpack the input asset's \"$assetLoanKey\" as $FigureTechLoanIdentifier"
                    }
                }
            }
        }
        "given an input to an empty scope with a loan value not of the expected MISMO loan type" should {
            "throw an appropriate exception" {
                Asset.newBuilder().also { assetBuilder ->
                    assetBuilder.putKv("mismoLoan", FigureTechUUID.getDefaultInstance().toProtoAny())
                }.build().let { assetWithBadLoanType ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordAsset(assetWithBadLoanType)
                    }.let { exception ->
                        exception.message shouldContain "Could not unpack the input asset's \"$assetMismoKey\" as $MISMOLoanIdentifier"
                    }
                }
            }
        }
        "given an input to an empty scope with invalid asset data" should {
            "throw an appropriate exception" {
                checkAll(anyInvalidUuid) { randomInvalidAssetId ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomInvalidAssetId
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Asset must have valid ID"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with invalid Figure Tech loan data" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyNonEmptyString, anyInvalidUuid) { randomAssetId, randomType, randomInvalidLoanId ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            "loan",
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomInvalidLoanId
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Loan must have valid ID"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with invalid MISMO loan data" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyNonEmptyString, anyNonUliString) { randomAssetId, randomType, randomInvalidUli ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            "mismoLoan",
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomInvalidUli
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Loan ULI is invalid"
                        }
                    }
                }
            }
        }
        "given an input with invalid changes to the existing asset's ID" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyUuid, anyNonEmptyString) { randomExistingUuid, randomNewUuid, randomLoanId, randomAssetType ->
                    val unchangedLoan = FigureTechLoan.newBuilder().also { loanBuilder ->
                        loanBuilder.id = randomLoanId
                    }.build().toProtoAny()
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomExistingUuid
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv("loan", unchangedLoan)
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomNewUuid
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv("loan", unchangedLoan)
                    }.build()
                    if (randomExistingUuid != randomNewUuid) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(newAsset)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Cannot change asset ID"
                        }
                    }
                }
            }
        }
        "given an input with invalid changes to the existing asset's type" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyNonEmptyString, anyNonEmptyString) { randomAssetId, randomLoanId, randomExistingType, randomNewType ->
                    val unchangedLoan = FigureTechLoan.newBuilder().also { loanBuilder ->
                        loanBuilder.id = randomLoanId
                    }.build().toProtoAny()
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.type = randomExistingType
                        assetBuilder.putKv("loan", unchangedLoan)
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomNewType
                        assetBuilder.putKv("loan", unchangedLoan)
                    }.build()
                    if (randomExistingType != randomNewType) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(newAsset)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Cannot change asset type"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with more than one type of loan" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an input with more than one type of loan to update an existing asset record" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given an input with a loan of a different type than the existing Figure Tech loan" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyValidUli) { randomAssetId, randomLoanId, randomUli ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            "loan",
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomLoanId
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            "loan",
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    shouldThrow<ContractViolationException> {
                        RecordLoanContract(
                            existingAsset = existingAsset,
                            // The rest of the parameters are not relevant to this test case
                            existingENote = ENote.getDefaultInstance(),
                            existingServicingData = ServicingData.getDefaultInstance(),
                            existingServicingRights = ServicingRights.getDefaultInstance(),
                        ).recordAsset(newAsset)
                    }.let { exception ->
                        exception.message shouldContain
                            "Expected input asset's \"$assetLoanKey\" to be a $FigureTechLoanIdentifier but was actually a $MISMOLoanIdentifier"
                    }
                }
            }
        }
        "given an input with a loan of a different type than the existing MISMO loan" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyValidUli) { randomAssetId, randomLoanId, randomUli ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            "mismoLoan",
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            "mismoLoan",
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomLoanId
                            }.build().toProtoAny()
                        )
                    }.build()
                    shouldThrow<ContractViolationException> {
                        RecordLoanContract(
                            existingAsset = existingAsset,
                            // The rest of the parameters are not relevant to this test case
                            existingENote = ENote.getDefaultInstance(),
                            existingServicingData = ServicingData.getDefaultInstance(),
                            existingServicingRights = ServicingRights.getDefaultInstance(),
                        ).recordAsset(newAsset)
                    }.let { exception ->
                        exception.message shouldContain
                            "Expected input asset's \"$assetMismoKey\" to be a $MISMOLoanIdentifier but was actually a $FigureTechLoanIdentifier"
                    }
                }
            }
        }
        "given an input with invalid changes to the existing Figure Tech loan" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyUuid) { randomExistingUuid, randomNewUuid, randomAssetId ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            "loan",
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomExistingUuid
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            "loan",
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomNewUuid
                            }.build().toProtoAny()
                        )
                    }.build()
                    if (randomExistingUuid != randomNewUuid) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(newAsset)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Cannot change loan ID"
                        }
                    }
                }
            }
        }
        "given an input with invalid changes to the existing MISMO loan" should {
            "throw an appropriate exception" {
                checkAll(anyValidUli, anyValidUli, anyUuid) { randomExistingUli, randomNewUli, randomAssetId ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            "mismoLoan",
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomExistingUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            "mismoLoan",
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomNewUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    if (randomExistingUli != randomNewUli) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(newAsset)
                        }.let { exception ->
                            exception.message shouldContain "Cannot change loan ULI"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with a valid Figure Tech loan" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString, anyValidFigureTechLoan) { randomAssetId, randomType, randomLoan ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv("loan", randomLoan.toProtoAny())
                        }.build()
                    )
                }
            }
        }
        "given a valid Figure Tech loan input to update an existing asset record" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given an input to an empty scope with a valid MISMO loan" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString, anyValidMismoLoan) { randomAssetId, randomType, randomLoan ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv("mismoLoan", randomLoan.toProtoAny())
                        }.build()
                    )
                }
            }
        }
        "given an valid MISMO loan input to update an existing asset record" xshould {
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
        "given an input to an empty scope with a valid eNote" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
