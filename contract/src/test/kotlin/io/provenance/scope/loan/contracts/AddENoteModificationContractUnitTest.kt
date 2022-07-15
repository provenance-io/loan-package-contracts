package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuidSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidChecksum
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENote
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.breakOffLast
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.toPair
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.util.v1beta1.DocumentMetadata
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum

class AddENoteModificationContractUnitTest : WordSpec({
    "addModification" When {
        val maxExistingModificationCount = (if (KotestConfig.runTestsExtended) 20 else 5)
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote()) { randomExistingENote ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document is not set"
                    }
                }
            }
        }
        "given an input with an invalid ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidChecksum,
                    anyInvalidUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksum, randomInvalidId, randomUri, randomContentType, randomDocumentType, randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomInvalidId
                                modificationBuilder.checksum = randomChecksum
                                modificationBuilder.uri = randomUri
                                modificationBuilder.contentType = randomContentType
                                modificationBuilder.documentType = randomDocumentType
                                modificationBuilder.fileName = randomFileName
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document must have valid ID"
                    }
                }
            }
        }
        "given an input without a valid checksum" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyNonEmptyString,
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksumAlgorithm, randomId, randomUri, randomContentType, randomDocumentType, randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomId
                                modificationBuilder.checksum = FigureTechChecksum.newBuilder().also { checksumBuilder ->
                                    checksumBuilder.clearChecksum()
                                    checksumBuilder.algorithm = randomChecksumAlgorithm
                                }.build()
                                modificationBuilder.uri = randomUri
                                modificationBuilder.contentType = randomContentType
                                modificationBuilder.documentType = randomDocumentType
                                modificationBuilder.fileName = randomFileName
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} must have a valid checksum string"
                    }
                }
            }
        }
        "given an input without a URI" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidChecksum,
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksum, randomId, randomContentType, randomDocumentType, randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomId
                                modificationBuilder.checksum = randomChecksum
                                modificationBuilder.contentType = randomContentType
                                modificationBuilder.documentType = randomDocumentType
                                modificationBuilder.fileName = randomFileName
                                modificationBuilder.clearUri()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} is missing URI"
                    }
                }
            }
        }
        "given an input without a content type" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidChecksum,
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksum, randomId, randomUri, randomDocumentType, randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomId
                                modificationBuilder.checksum = randomChecksum
                                modificationBuilder.uri = randomUri
                                modificationBuilder.documentType = randomDocumentType
                                modificationBuilder.fileName = randomFileName
                                modificationBuilder.clearContentType()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} is missing content type"
                    }
                }
            }
        }
        "given an input without a document type" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidChecksum,
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksum, randomId, randomContentType, randomUri, randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomId
                                modificationBuilder.checksum = randomChecksum
                                modificationBuilder.contentType = randomContentType
                                modificationBuilder.uri = randomUri
                                modificationBuilder.fileName = randomFileName
                                modificationBuilder.clearDocumentType()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} is missing document type"
                    }
                }
            }
        }
        "given an input without a file name" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidENote(),
                    anyValidChecksum,
                    anyUuid,
                    anyNonEmptyString,
                    anyNonEmptyString,
                    anyNonEmptyString,
                ) { randomExistingENote, randomChecksum, randomId, randomContentType, randomDocumentType, randomUri ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = randomExistingENote,
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = randomId
                                modificationBuilder.checksum = randomChecksum
                                modificationBuilder.contentType = randomContentType
                                modificationBuilder.documentType = randomDocumentType
                                modificationBuilder.uri = randomUri
                                modificationBuilder.clearFileName()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} is missing file name"
                    }
                }
            }
        }
        "given an input which attempts to silently change properties of an existing modification" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 1, max = maxExistingModificationCount).flatMap { documentCount ->
                        anyValidENote(modificationCount = documentCount)
                    },
                    anyValidChecksum,
                    anyUuidSet(size = 2).toPair(),
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    anyNonEmptyString,
                ) { eNote, checksum, (oldId, newId), (oldAl, newAl), (oldUri, newUri), (oldCType, newCType), (oldDType, newDType), randomFileName ->
                    shouldThrow<ContractViolationException> {
                        AddENoteModificationContract(
                            existingENote = eNote.toBuilder().also { existingENoteBuilder ->
                                existingENoteBuilder.addModification(
                                    DocumentMetadata.newBuilder().also { modificationBuilder ->
                                        modificationBuilder.id = oldId
                                        modificationBuilder.checksum = checksum.toBuilder().also { checksumBuilder ->
                                            checksumBuilder.algorithm = oldAl
                                        }.build()
                                        modificationBuilder.contentType = oldCType
                                        modificationBuilder.documentType = oldDType
                                        modificationBuilder.fileName = randomFileName
                                        modificationBuilder.uri = oldUri
                                    }.build()
                                )
                            }.build(),
                        ).addModification(
                            DocumentMetadata.newBuilder().also { modificationBuilder ->
                                modificationBuilder.id = newId
                                modificationBuilder.checksum = checksum.toBuilder().also { checksumBuilder ->
                                    checksumBuilder.algorithm = newAl
                                }.build()
                                modificationBuilder.contentType = newCType
                                modificationBuilder.documentType = newDType
                                modificationBuilder.uri = newUri
                                modificationBuilder.fileName = randomFileName
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 5
                        listOf("checksum algorithm", "ID", "URI", "content type", "document type").forEach { immutableField ->
                            exception.message shouldContain
                                "Cannot change $immutableField of existing document with checksum ${checksum.checksum}"
                        }
                    }
                }
            }
        }
        "given an input which duplicates an existing modification's properties with the exception of the file name" should {
            // TODO: Revisit this behavior in the future
            "be added to the list without removing the existing modification" {
                checkAll(
                    anyValidENote(modificationCount = 0),
                    Arb.int(min = 1, max = maxExistingModificationCount).flatMap { documentCount ->
                        anyValidDocumentSet(size = documentCount)
                    },
                    anyNonEmptyString,
                ) { randomExistingENote, randomExistingModifications, randomFileName ->
                    AddENoteModificationContract(
                        existingENote = randomExistingENote.toBuilder().also { existingENoteBuilder ->
                            existingENoteBuilder.addAllModification(randomExistingModifications)
                        }.build(),
                    ).addModification(
                        randomExistingModifications.takeLast(1)[0].toBuilder().also { newModificationBuilder ->
                            newModificationBuilder.fileName = randomFileName
                        }.build()
                    ).let { updatedENote ->
                        updatedENote.modificationCount shouldBe randomExistingModifications.size + 1
                        updatedENote.modificationList.last().fileName shouldBe randomFileName
                    }
                }
            }
        }
        "given a valid input with a completely unique modification" should {
            "not throw an exception" {
                checkAll(
                    anyValidENote(modificationCount = 0),
                    Arb.int(min = 1, max = maxExistingModificationCount).flatMap { documentCount ->
                        anyValidDocumentSet(size = documentCount)
                    },
                ) { randomExistingENote, randomModifications, ->
                    val (existingModifications, newModification) = randomModifications.breakOffLast()
                    AddENoteModificationContract(
                        existingENote = randomExistingENote.toBuilder().also { existingENoteBuilder ->
                            existingENoteBuilder.addAllModification(existingModifications)
                        }.build(),
                    ).addModification(
                        newModification
                    ).let { updatedENote ->
                        updatedENote.modificationCount shouldBe randomModifications.size
                        updatedENote.modificationList.last() shouldBe newModification
                    }
                }
            }
        }
    }
})
