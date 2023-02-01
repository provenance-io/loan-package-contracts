package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.checkAll
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.LoanScopeProperties.assetMismoKey
import io.provenance.scope.loan.test.Constructors.recordContractWithEmptyScope
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidAsset
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidFigureTechLoan
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidMismoLoan
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonUliString
import io.provenance.scope.loan.test.PrimitiveArbs.anyValidUli
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.toFigureTechLoan
import io.provenance.scope.loan.utility.toMISMOLoan
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
                shouldThrow<ContractViolationException> {
                    recordContractWithEmptyScope.recordAsset(Asset.getDefaultInstance())
                }.let { exception ->
                    exception shouldHaveViolationCount 3
                    exception.message shouldContainIgnoringCase "Asset must have valid ID"
                    exception.message shouldContainIgnoringCase "Asset is missing type"
                    exception.message shouldContainIgnoringCase "\"$assetLoanKey\" must be a key in the input asset"
                }
            }
        }
        "given an input to an empty scope with a loan value not of the expected Figure Tech loan type" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyNonEmptyString) { randomAssetId, randomAssetType ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(assetLoanKey, FigureTechUUID.getDefaultInstance().toProtoAny())
                    }.build().let { assetWithBadLoanType ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithBadLoanType)
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "Could not unpack the input asset's \"$assetLoanKey\" as $FigureTechLoanIdentifier"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with an invalid MISMO loan value and no Figure Tech loan type" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyNonEmptyString) { randomAssetId, randomAssetType ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(assetMismoKey, FigureTechUUID.getDefaultInstance().toProtoAny())
                    }.build().let { assetWithBadLoanType ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithBadLoanType)
                        }.let { exception ->
                            exception shouldHaveViolationCount 2
                            exception.message shouldContain "\"loan\" must be a key in the input asset"
                            exception.message shouldContain "Could not unpack the input asset's \"$assetMismoKey\" as $MISMOLoanIdentifier"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with an invalid asset ID" should {
            "throw an appropriate exception" {
                checkAll(anyInvalidUuid, anyNonEmptyString) { randomInvalidAssetId, randomAssetType ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomInvalidAssetId
                        assetBuilder.type = randomAssetType
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
        "given a Figure Tech loan input to an empty scope with an invalid ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyInvalidUuid,
                ) { randomAssetId, randomType, randomLoan, randomInvalidLoanId ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            assetLoanKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.id = randomInvalidLoanId
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContainIgnoringCase "Loan must have valid ID"
                        }
                    }
                }
            }
        }
        "given a Figure Tech loan input to an empty scope with an invalid originator ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyInvalidUuid,
                ) { randomAssetId, randomType, randomLoan, randomInvalidOriginatorId ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            assetLoanKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.originatorUuid = randomInvalidOriginatorId
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContainIgnoringCase "Loan must have valid originator ID"
                        }
                    }
                }
            }
        }
        "given a Figure Tech loan input to an empty scope without an originator name" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                ) { randomAssetId, randomType, randomLoan ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            assetLoanKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.clearOriginatorName()
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContainIgnoringCase "Loan is missing originator name"
                        }
                    }
                }
            }
        }
        "given a Figure Tech loan input to an empty scope with a ULI of an invalid length" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyNonUliString,
                ) { randomAssetId, randomType, randomLoan, randomInvalidUli ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            assetLoanKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomInvalidUli
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Loan ULI must be between 23 and 45 (inclusive) characters long"
                        }
                    }
                }
            }
        }
        "given a MISMO loan input to an empty scope with a ULI of an invalid length" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidMismoLoan,
                    anyNonUliString,
                ) { randomAssetId, randomType, randomLoan, randomInvalidUli ->
                    Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomType
                        assetBuilder.putKv(
                            assetMismoKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomInvalidUli
                            }.build().toProtoAny()
                        )
                    }.build().let { assetWithInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordContractWithEmptyScope.recordAsset(assetWithInvalidId)
                        }.let { exception ->
                            exception.message shouldContainIgnoringCase "Loan ULI must be between 23 and 45 (inclusive) characters long"
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
                        assetBuilder.putKv(assetLoanKey, unchangedLoan)
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomNewUuid
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(assetLoanKey, unchangedLoan)
                    }.build()
                    if (randomExistingUuid != randomNewUuid) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(
                                newAsset = newAsset
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
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
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomExistingType
                        assetBuilder.putKv(assetLoanKey, unchangedLoan)
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomNewType
                        assetBuilder.putKv(assetLoanKey, unchangedLoan)
                    }.build()
                    if (randomExistingType != randomNewType) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(
                                newAsset = newAsset
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContainIgnoringCase "Cannot change asset type"
                        }
                    }
                }
            }
        }
        "given an input with a loan of a different type than the existing Figure Tech loan" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyValidUli) { randomAssetId, randomLoanId, randomUli ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            assetLoanKey,
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomLoanId
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            assetLoanKey,
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
                        ).recordAsset(
                            newAsset = newAsset
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Expected input asset's \"$assetLoanKey\" to be a $FigureTechLoanIdentifier but was actually a $MISMOLoanIdentifier"
                    }
                }
            }
        }
        "given an input with a MISMO loan of a different type than the existing MISMO loan" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyUuid,
                    anyValidUli,
                    anyValidFigureTechLoan,
                ) { randomAssetId, randomLoanId, randomUli, randomLoan ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            assetMismoKey,
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomUli
                            }.build().toProtoAny()
                        )
                        assetBuilder.putKv(assetLoanKey, randomLoan.toProtoAny())
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            assetMismoKey,
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomLoanId
                            }.build().toProtoAny()
                        )
                        assetBuilder.putKv(assetLoanKey, randomLoan.toProtoAny())
                    }.build()
                    shouldThrow<ContractViolationException> {
                        RecordLoanContract(
                            existingAsset = existingAsset,
                            // The rest of the parameters are not relevant to this test case
                            existingENote = ENote.getDefaultInstance(),
                            existingServicingData = ServicingData.getDefaultInstance(),
                            existingServicingRights = ServicingRights.getDefaultInstance(),
                        ).recordAsset(
                            newAsset = newAsset
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Expected input asset's \"$assetMismoKey\" to be a $MISMOLoanIdentifier but was actually a $FigureTechLoanIdentifier"
                    }
                }
            }
        }
        "given an input with invalid changes to the existing Figure Tech loan's ID" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyUuid, anyUuid) { randomExistingUuid, randomNewUuid, randomAssetId ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId // To mark the existing asset as being set
                        assetBuilder.putKv(
                            assetLoanKey,
                            FigureTechLoan.newBuilder().also { loanBuilder ->
                                loanBuilder.id = randomExistingUuid
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            assetLoanKey,
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
                            ).recordAsset(
                                newAsset = newAsset
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContainIgnoringCase "Cannot change loan ID"
                        }
                    }
                }
            }
        }
        "given an input with invalid changes to the existing MISMO loan's ULI" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidUli,
                    anyValidUli,
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyValidMismoLoan,
                ) { randomExistingUli, randomNewUli, randomAssetId, randomAssetType, randomLoan, randomMismoLoan ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(
                            assetMismoKey,
                            randomMismoLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomExistingUli
                            }.build().toProtoAny()
                        )
                        assetBuilder.putKv(assetLoanKey, randomLoan.toProtoAny())
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(
                            assetMismoKey,
                            randomMismoLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomNewUli
                            }.build().toProtoAny()
                        )
                        assetBuilder.putKv(assetLoanKey, randomLoan.toProtoAny())
                    }.build()
                    if (randomExistingUli != randomNewUli) {
                        shouldThrow<ContractViolationException> {
                            RecordLoanContract(
                                existingAsset = existingAsset,
                                // The rest of the parameters are not relevant to this test case
                                existingENote = ENote.getDefaultInstance(),
                                existingServicingData = ServicingData.getDefaultInstance(),
                                existingServicingRights = ServicingRights.getDefaultInstance(),
                            ).recordAsset(
                                newAsset = newAsset
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "Cannot change loan ULI"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with a valid Figure Tech loan and no MISMO loan" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString, anyValidFigureTechLoan) { randomAssetId, randomType, randomLoan ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv(assetLoanKey, randomLoan.toProtoAny())
                        }.build()
                    ).let { newAsset ->
                        newAsset.id shouldBe randomAssetId
                        newAsset.type shouldBe randomType
                        newAsset.kvMap[assetLoanKey]?.toFigureTechLoan() shouldBe randomLoan
                    }
                }
            }
        }
        "given a valid Figure Tech loan input to update an existing asset record" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given an asset which removes the MISMO loan from an existing loan record" should {
            "not throw an exception" {
                checkAll(
                    anyValidAsset(hasMismoLoan = true),
                ) { randomExistingAsset ->
                    val randomNewAsset = randomExistingAsset.toBuilder().also { newAssetBuilder ->
                        newAssetBuilder.removeKv(assetMismoKey)
                    }.build()
                    RecordLoanContract(
                        existingAsset = randomExistingAsset,
                        // The rest of the parameters are not relevant to this test case
                        existingENote = ENote.getDefaultInstance(),
                        existingServicingData = ServicingData.getDefaultInstance(),
                        existingServicingRights = ServicingRights.getDefaultInstance(),
                    ).recordAsset(
                        newAsset = randomNewAsset
                    ).let { resultingNewAsset ->
                        resultingNewAsset.id shouldBe randomExistingAsset.id
                        resultingNewAsset.type shouldBe randomExistingAsset.type
                        resultingNewAsset.kvMap[assetMismoKey] shouldBe null
                    }
                }
            }
        }
        "given an asset which adds a MISMO loan to an existing loan record" should {
            "not throw an exception" {
                checkAll(
                    anyValidAsset(hasMismoLoan = true),
                ) { randomNewAsset ->
                    val randomExistingAsset = randomNewAsset.toBuilder().also { existingAssetBuilder ->
                        existingAssetBuilder.removeKv(assetMismoKey)
                    }.build()
                    RecordLoanContract(
                        existingAsset = randomExistingAsset,
                        // The rest of the parameters are not relevant to this test case
                        existingENote = ENote.getDefaultInstance(),
                        existingServicingData = ServicingData.getDefaultInstance(),
                        existingServicingRights = ServicingRights.getDefaultInstance(),
                    ).recordAsset(
                        newAsset = randomNewAsset
                    ).let { resultingNewAsset ->
                        resultingNewAsset.id shouldBe randomExistingAsset.id
                        resultingNewAsset.type shouldBe randomExistingAsset.type
                        resultingNewAsset.kvMap[assetMismoKey] shouldBe randomNewAsset.kvMap[assetMismoKey]
                    }
                }
            }
        }
        "given an input to an empty scope with a valid Figure Tech loan and valid MISMO loan" should {
            "not throw an exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyValidMismoLoan,
                ) { randomAssetId, randomType, randomFigureTechLoan, randomMismoLoan ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv(assetMismoKey, randomMismoLoan.toProtoAny())
                            putKv(assetLoanKey, randomFigureTechLoan.toProtoAny())
                        }.build()
                    ).let { newAsset ->
                        newAsset.id shouldBe randomAssetId
                        newAsset.type shouldBe randomType
                        newAsset.kvMap[assetLoanKey]?.toFigureTechLoan() shouldBe randomFigureTechLoan
                        newAsset.kvMap[assetMismoKey]?.toMISMOLoan() shouldBe randomMismoLoan
                    }
                }
            }
        }
        "given an valid MISMO loan input to update an existing asset record" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given an input with a MISMO loan but no Figure Tech loan to update an existing Figure Tech loan record" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidAsset(hasMismoLoan = true),
                ) { randomBaseAsset ->
                    val randomExistingAsset = randomBaseAsset.toBuilder().also { existingAssetBuilder ->
                        existingAssetBuilder.removeKv(assetMismoKey)
                    }.build()
                    val randomNewAsset = randomBaseAsset.toBuilder().also { existingAssetBuilder ->
                        existingAssetBuilder.removeKv(assetLoanKey)
                    }.build()
                    shouldThrow<ContractViolationException> {
                        RecordLoanContract(
                            existingAsset = randomExistingAsset,
                            // The rest of the parameters are not relevant to this test case
                            existingENote = ENote.getDefaultInstance(),
                            existingServicingData = ServicingData.getDefaultInstance(),
                            existingServicingRights = ServicingRights.getDefaultInstance(),
                        ).recordAsset(
                            newAsset = randomNewAsset
                        )
                    }.let { exception ->
                        exception.message shouldContain "\"${assetLoanKey}\" must be a key in the input asset"
                    }
                }
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
