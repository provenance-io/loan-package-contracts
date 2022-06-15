package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
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
                    exception shouldHaveViolationCount 3U
                    exception.message shouldContainIgnoringCase "Asset must have valid ID"
                    exception.message shouldContainIgnoringCase "Asset is missing type"
                    exception.message shouldContainIgnoringCase
                        "Exactly one of \"$assetLoanKey\" or \"$assetMismoKey\" must be a key in the input asset"
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
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContain "Could not unpack the input asset's \"$assetLoanKey\" as $FigureTechLoanIdentifier"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with a loan value not of the expected MISMO loan type" should {
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
                            exception shouldHaveViolationCount 1U
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
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContainIgnoringCase "Loan must have valid ID"
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
                            exception shouldHaveViolationCount 1U
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
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContainIgnoringCase "Cannot change asset type"
                        }
                    }
                }
            }
        }
        "given an input to an empty scope with more than one type of loan" should {
            "throw an appropriate exception" {
                checkAll(
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyValidMismoLoan
                ) { randomAssetId, randomAssetType, randomFigureTechLoan, randomMismoLoan ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordAsset(
                            Asset.newBuilder().also { assetBuilder ->
                                assetBuilder.id = randomAssetId
                                assetBuilder.type = randomAssetType
                                assetBuilder.putKv(assetLoanKey, randomFigureTechLoan.toProtoAny())
                                assetBuilder.putKv(assetMismoKey, randomMismoLoan.toProtoAny())
                            }.build()
                        )
                    }.let { exception ->
                        exception.message shouldContain "Exactly one of \"$assetLoanKey\" or \"$assetMismoKey\" must be a key in the input asset"
                    }
                }
            }
        }
        "given an input with more than one type of loan to update an existing asset record" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.choice(anyValidAsset<FigureTechLoan>(), anyValidAsset<MISMOLoanMetadata>()),
                    anyUuid,
                    anyNonEmptyString,
                    anyValidFigureTechLoan,
                    anyValidMismoLoan
                ) { randomExistingAsset, randomAssetId, randomAssetType, randomFigureTechLoan, randomMismoLoan ->
                    shouldThrow<ContractViolationException> {
                        RecordLoanContract(
                            existingAsset = randomExistingAsset,
                            // The rest of the parameters are not relevant to this test case
                            existingENote = ENote.getDefaultInstance(),
                            existingServicingData = ServicingData.getDefaultInstance(),
                            existingServicingRights = ServicingRights.getDefaultInstance(),
                        )
                        recordContractWithEmptyScope.recordAsset(
                            Asset.newBuilder().also { assetBuilder ->
                                assetBuilder.id = randomAssetId
                                assetBuilder.type = randomAssetType
                                assetBuilder.putKv(assetLoanKey, randomFigureTechLoan.toProtoAny())
                                assetBuilder.putKv(assetMismoKey, randomMismoLoan.toProtoAny())
                            }.build()
                        )
                    }.let { exception ->
                        exception.message shouldContain "Exactly one of \"$assetLoanKey\" or \"$assetMismoKey\" must be a key in the input asset"
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
                        exception shouldHaveViolationCount 1U
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
                            assetMismoKey,
                            MISMOLoanMetadata.newBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.putKv(
                            assetMismoKey,
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
                        ).recordAsset(
                            newAsset = newAsset
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
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
                            exception shouldHaveViolationCount 1U
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
                    anyValidMismoLoan,
                ) { randomExistingUli, randomNewUli, randomAssetId, randomAssetType, randomLoan ->
                    val existingAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(
                            assetMismoKey,
                            randomLoan.toBuilder().also { loanBuilder ->
                                loanBuilder.uli = randomExistingUli
                            }.build().toProtoAny()
                        )
                    }.build()
                    val newAsset = Asset.newBuilder().also { assetBuilder ->
                        assetBuilder.id = randomAssetId
                        assetBuilder.type = randomAssetType
                        assetBuilder.putKv(
                            assetMismoKey,
                            randomLoan.toBuilder().also { loanBuilder ->
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
                            ).recordAsset(
                                newAsset = newAsset
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1U
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
                            putKv(assetLoanKey, randomLoan.toProtoAny())
                        }.build()
                    ).let { newAsset ->
                        newAsset.id shouldBe randomAssetId
                        newAsset.type shouldBe randomType
                        /* We don't use anyValidAsset<FigureTechLoan>() for this test case so that we can make the following check useful */
                        newAsset.kvMap[assetLoanKey]!!.toFigureTechLoan() shouldBe randomLoan
                    }
                }
            }
        }
        "given a valid Figure Tech loan input to update an existing asset record" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given a Figure Tech loan input to update an existing MISMO loan record" should {
            "throw an appropriate exception" {
                checkAll(anyValidAsset<MISMOLoanMetadata>(), anyValidAsset<FigureTechLoan>()) { randomExistingAsset, randomNewAsset ->
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
                        exception.message shouldContain "The input asset had key \"${assetLoanKey}\" but the existing asset did not"
                    }
                }
            }
        }
        "given an input to an empty scope with a valid MISMO loan" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString, anyValidMismoLoan) { randomAssetId, randomType, randomLoan ->
                    recordContractWithEmptyScope.recordAsset(
                        Asset.newBuilder().apply {
                            id = randomAssetId
                            type = randomType
                            putKv(assetMismoKey, randomLoan.toProtoAny())
                        }.build()
                    ).let { newAsset ->
                        newAsset.id shouldBe randomAssetId
                        newAsset.type shouldBe randomType
                        /* We don't use anyValidAsset<MISMOLoanMetadata>() for this test case so that we can make the following check useful */
                        newAsset.kvMap[assetMismoKey]!!.toMISMOLoan() shouldBe randomLoan
                    }
                }
            }
        }
        "given an valid MISMO loan input to update an existing asset record" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given a MISMO loan input to update an existing Figure Tech loan record" should {
            "throw an appropriate exception" {
                checkAll(anyValidAsset<FigureTechLoan>(), anyValidAsset<MISMOLoanMetadata>()) { randomExistingAsset, randomNewAsset ->
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
                        exception.message shouldContain "The input asset had key \"${assetMismoKey}\" but the existing asset did not"
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
