package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyChecksumSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyPastNonEpochDate
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentMetadata
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENote
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENoteController
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanDocumentSet
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.toPair
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.util.v1beta1.DocumentMetadata
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum

class UpdateENoteContractUnitTest : WordSpec({
    "updateENote" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote()) { randomExistingENote ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote,
                        ).updateENote(
                            DocumentMetadata.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote document is not set"
                    }
                }
            }
        }
        "given an input with an invalid ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidDocumentMetadata,
                    anyChecksumSet(size = 2).toPair(),
                    anyInvalidUuid,
                ) { randomExistingENote, randomNewENote, (randomExistingChecksum, randomNewChecksum), randomInvalidId ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = eNoteBuilder.eNote.toBuilder().also { documentBuilder ->
                                    documentBuilder.checksum = randomExistingChecksum
                                }.build()
                            }.build(),
                        ).updateENote(
                            randomNewENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.id = randomInvalidId
                                eNoteBuilder.checksum = randomNewChecksum
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote must have valid ID"
                    }
                }
            }
        }
        "given an input without a URI" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidDocumentMetadata,
                    anyChecksumSet(size = 2).toPair(),
                ) { randomExistingENote, randomNewENote, (randomExistingChecksum, randomNewChecksum) ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = eNoteBuilder.eNote.toBuilder().also { documentBuilder ->
                                    documentBuilder.checksum = randomExistingChecksum
                                }.build()
                            }.build(),
                        ).updateENote(
                            randomNewENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.checksum = randomNewChecksum
                                eNoteBuilder.clearUri()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote is missing URI"
                    }
                }
            }
        }
        "given an input without a content type" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidDocumentMetadata,
                    anyChecksumSet(size = 2).toPair(),
                ) { randomExistingENote, randomNewENote, (randomExistingChecksum, randomNewChecksum) ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = eNoteBuilder.eNote.toBuilder().also { documentBuilder ->
                                    documentBuilder.checksum = randomExistingChecksum
                                }.build()
                            }.build(),
                        ).updateENote(
                            randomNewENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.checksum = randomNewChecksum
                                eNoteBuilder.clearContentType()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote is missing content type"
                    }
                }
            }
        }
        "given an input without a document type" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidDocumentMetadata,
                    anyChecksumSet(size = 2).toPair(),
                ) { randomExistingENote, randomNewENote, (randomExistingChecksum, randomNewChecksum) ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = eNoteBuilder.eNote.toBuilder().also { documentBuilder ->
                                    documentBuilder.checksum = randomExistingChecksum
                                }.build()
                            }.build(),
                        ).updateENote(
                            randomNewENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.checksum = randomNewChecksum
                                eNoteBuilder.clearDocumentType()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote is missing document type"
                    }
                }
            }
        }
        "given an input without a valid checksum" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomId, randomUri, randomContentType, randomDocumentType, randomChecksumAlgorithm ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = randomExistingENote,
                        ).updateENote(
                            DocumentMetadata.newBuilder().also { eNoteBuilder ->
                                eNoteBuilder.id = randomId
                                eNoteBuilder.uri = randomUri
                                eNoteBuilder.contentType = randomContentType
                                eNoteBuilder.documentType = randomDocumentType
                                eNoteBuilder.checksum = FigureTechChecksum.newBuilder().also { checksumBuilder ->
                                    checksumBuilder.clearChecksum()
                                    checksumBuilder.algorithm = randomChecksumAlgorithm
                                }.build()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "eNote is missing checksum"
                    }
                }
            }
        }
        "given an input which attempts to silently change the URI of the existing eNote" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENoteController,
                    anyPastNonEpochDate,
                    anyNonEmptyString,
                    anyValidDocumentMetadata,
                ) { randomController, randomSignedDate, randomVaultName, randomENote ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteContract(
                            existingENote = ENote.newBuilder().also { eNoteBuilder ->
                                eNoteBuilder.eNote = randomENote
                                eNoteBuilder.controller = randomController
                                eNoteBuilder.signedDate = randomSignedDate
                                eNoteBuilder.vaultName = randomVaultName
                            }.build(),
                        ).updateENote(
                            newENote = randomENote.toBuilder().also { eNoteBuilder ->
                                eNoteBuilder.uri = "someNewDirectory/" + eNoteBuilder.uri
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Cannot change URI of existing document with checksum ${randomENote.checksum.checksum}"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(
                    anyValidENoteController,
                    anyPastNonEpochDate,
                    anyNonEmptyString,
                    anyValidLoanDocumentSet(size = 2).toPair { record -> record.documentList },
                ) { randomController, randomSignedDate, randomVaultName, (randomExistingENote, randomNewENote) ->
                    UpdateENoteContract(
                        existingENote = ENote.newBuilder().also { eNoteBuilder ->
                            eNoteBuilder.eNote = randomExistingENote
                            eNoteBuilder.controller = randomController
                            eNoteBuilder.signedDate = randomSignedDate
                            eNoteBuilder.vaultName = randomVaultName
                        }.build(),
                    ).updateENote(
                        newENote = randomNewENote
                    ).let { newENote ->
                        newENote.eNote shouldBe randomNewENote
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
