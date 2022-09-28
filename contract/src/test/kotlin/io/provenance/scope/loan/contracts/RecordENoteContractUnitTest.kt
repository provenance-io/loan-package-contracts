package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyAssetType
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyBorrowerInfo
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyNonNegativeMoney
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyPastNonEpochDate
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidChecksum
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentMetadata
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENoteController
import io.provenance.scope.loan.test.MetadataAssetModelArbs.loanStateSet
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.isSet
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.util.v1beta1.Date
import java.time.LocalDate

class RecordENoteContractUnitTest : WordSpec({
    /**
     * This test class should only need to test for an empty scope as long as all functions of RecordENoteContract use @SkipIfRecordExists
     */
    RecordENoteContract(
        existingENote = ENote.getDefaultInstance(),
        existingServicingData = ServicingData.getDefaultInstance(),
    ).run {
        "recordENote for an empty scope" When {
            "given an empty input" should {
                "throw an appropriate exception" {
                    shouldThrow<ContractViolationException> {
                        recordENote(ENote.getDefaultInstance())
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "eNote is not set"
                    }
                }
            }
            "given an input without a controller" should {
                "throw an appropriate exception" {
                    checkAll(anyValidDocumentMetadata, anyPastNonEpochDate, anyNonEmptyString) { randomDocument, randomSignedDate, randomVaultName ->
                        shouldThrow<ContractViolationException> {
                            recordENote(
                                ENote.newBuilder().also { eNoteBuilder ->
                                    eNoteBuilder.clearController()
                                    eNoteBuilder.eNote = randomDocument
                                    eNoteBuilder.signedDate = randomSignedDate
                                    eNoteBuilder.vaultName = randomVaultName
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "Controller is not set"
                        }
                    }
                }
            }
            "given an input without an eNote" should {
                "throw an appropriate exception" {
                    checkAll(anyValidENoteController, anyPastNonEpochDate, anyNonEmptyString) { randomController, randomSignedDate, randomVaultName ->
                        shouldThrow<ContractViolationException> {
                            recordENote(
                                ENote.newBuilder().also { eNoteBuilder ->
                                    eNoteBuilder.clearENote()
                                    eNoteBuilder.controller = randomController
                                    eNoteBuilder.signedDate = randomSignedDate
                                    eNoteBuilder.vaultName = randomVaultName
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "eNote document is not set"
                        }
                    }
                }
            }
            "given an input without a signed date" should {
                "throw an appropriate exception" {
                    checkAll(anyValidENoteController, anyValidDocumentMetadata, anyNonEmptyString) { randomController, randomENote, randomVaultName ->
                        shouldThrow<ContractViolationException> {
                            recordENote(
                                ENote.newBuilder().also { eNoteBuilder ->
                                    eNoteBuilder.clearSignedDate()
                                    eNoteBuilder.controller = randomController
                                    eNoteBuilder.eNote = randomENote
                                    eNoteBuilder.vaultName = randomVaultName
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "eNote must have valid signed date"
                        }
                    }
                }
            }
            "given an input with a signed date in the future" should {
                "throw an appropriate exception" {
                    checkAll(
                        anyValidENoteController,
                        anyValidDocumentMetadata,
                        anyNonEmptyString,
                        Arb.localDate(
                            minDate = LocalDate.now().plusDays(1L),
                        ).map { javaLocalDate ->
                            Date.newBuilder().also { dateBuilder ->
                                dateBuilder.value = javaLocalDate.toString()
                            }.build()
                        }
                    ) { randomController, randomENote, randomVaultName, randomInvalidSignedDate ->
                        shouldThrow<ContractViolationException> {
                            recordENote(
                                ENote.newBuilder().also { eNoteBuilder ->
                                    eNoteBuilder.controller = randomController
                                    eNoteBuilder.eNote = randomENote
                                    eNoteBuilder.vaultName = randomVaultName
                                    eNoteBuilder.signedDate = randomInvalidSignedDate
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "eNote must have valid signed date"
                        }
                    }
                }
            }
            "given an input with duplicate signature checksums" should {
                "throw an appropriate exception" {
                    checkAll(
                        anyValidENoteController,
                        anyValidDocumentMetadata,
                        anyNonEmptyString,
                        anyPastNonEpochDate,
                        anyValidDocumentSet(size = 2),
                        anyValidChecksum,
                    ) { randomController, randomENote, randomVaultName, randomSignedDate, randomSignatures, randomDuplicateChecksum ->
                        shouldThrow<ContractViolationException> {
                            recordENote(
                                ENote.newBuilder().also { eNoteBuilder ->
                                    eNoteBuilder.controller = randomController
                                    eNoteBuilder.eNote = randomENote
                                    eNoteBuilder.vaultName = randomVaultName
                                    eNoteBuilder.signedDate = randomSignedDate
                                    eNoteBuilder.clearBorrowerSignatureImage()
                                    eNoteBuilder.addAllBorrowerSignatureImage(
                                        randomSignatures.map { signature ->
                                            signature.toBuilder().also { signatureBuilder ->
                                                signatureBuilder.checksum = randomDuplicateChecksum
                                            }.build()
                                        }
                                    )
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain
                                "Borrower signature with checksum ${randomDuplicateChecksum.checksum} is provided more than once in input"
                        }
                    }
                }
            }
            "given a valid input" should {
                "not throw an exception" {
                    checkAll(
                        anyValidDocumentMetadata,
                        anyValidENoteController,
                        anyPastNonEpochDate,
                        anyNonEmptyString,
                    ) { randomDocument, randomController, randomSignedDate, randomVaultName ->
                        recordENote(
                            ENote.newBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = randomDocument
                                eNoteBuilder.controller = randomController
                                eNoteBuilder.signedDate = randomSignedDate
                                eNoteBuilder.vaultName = randomVaultName
                            }.build()
                        ).let { newENote ->
                            newENote.eNote.isSet() shouldBe true
                            newENote.controller.isSet() shouldBe true
                            newENote.signedDate.isSet() shouldBe true
                            newENote.vaultName.isNotBlank() shouldBe true
                        }
                    }
                }
            }
        }
        "recordServicingData for an empty scope" When {
            "given an empty input" should {
                "throw an appropriate exception" {
                    shouldThrow<ContractViolationException> {
                        recordServicingData(ServicingData.getDefaultInstance())
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Servicing data is not set"
                    }
                }
            }
            "given an input with an invalid document" should {
                "throw an appropriate exception" {
                    checkAll(
                        loanStateSet(size = 1),
                        anyValidDocumentMetadata,
                        anyValidDocumentMetadata,
                        anyInvalidUuid,
                    ) { randomLoanState, randomFirstValidDocument, randomSecondDocument, randomInvalidId ->
                        shouldThrow<ContractViolationException> {
                            recordServicingData(
                                ServicingData.newBuilder().also { servicingDataBuilder ->
                                    servicingDataBuilder.addAllLoanState(randomLoanState)
                                    servicingDataBuilder.clearDocMeta()
                                    servicingDataBuilder.addDocMeta(randomFirstValidDocument)
                                    servicingDataBuilder.addDocMeta(
                                        randomSecondDocument.toBuilder().also { invalidDocumentBuilder ->
                                            invalidDocumentBuilder.id = randomInvalidId
                                        }.build()
                                    )
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1
                            exception.message shouldContain "Document must have valid ID"
                        }
                    }
                }
            }
            "given a valid input" should {
                "not throw an exception" {
                    checkAll(
                        anyUuid,
                        anyAssetType,
                        anyBorrowerInfo(additionalBorrowerCount = 0..1),
                        anyNonNegativeMoney,
                        Arb.int(min = 1, max = if (KotestConfig.runTestsExtended) 100 else 10).flatMap { randomLoanDocumentCount ->
                            anyValidDocumentSet(size = randomLoanDocumentCount, slippage = 70)
                        },
                        Arb.int(min = 1, max = if (KotestConfig.runTestsExtended) 100 else 10).flatMap { randomLoanStateCount ->
                            loanStateSet(size = randomLoanStateCount, slippage = 70)
                        },
                    ) { randomLoanId, randomAssetType, randomBorrowerInfo, randomOriginalNoteAmount, randomServicingDocuments, randomLoanStates ->
                        recordServicingData(
                            ServicingData.newBuilder().also { servicingDataBuilder ->
                                servicingDataBuilder.loanId = randomLoanId
                                servicingDataBuilder.assetType = randomAssetType
                                servicingDataBuilder.currentBorrowerInfo = randomBorrowerInfo
                                servicingDataBuilder.originalNoteAmount = randomOriginalNoteAmount
                                servicingDataBuilder.clearDocMeta()
                                servicingDataBuilder.addAllDocMeta(randomServicingDocuments)
                                servicingDataBuilder.clearLoanState()
                                servicingDataBuilder.addAllLoanState(randomLoanStates)
                            }.build()
                        ).let { newServicingData ->
                            newServicingData.loanId shouldBe randomLoanId
                            newServicingData.assetType shouldBe randomAssetType
                            newServicingData.currentBorrowerInfo shouldBe randomBorrowerInfo
                            newServicingData.originalNoteAmount shouldBe randomOriginalNoteAmount
                            newServicingData.docMetaCount shouldBeExactly randomServicingDocuments.size
                            newServicingData.loanStateCount shouldBeExactly randomLoanStates.size
                        }
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
