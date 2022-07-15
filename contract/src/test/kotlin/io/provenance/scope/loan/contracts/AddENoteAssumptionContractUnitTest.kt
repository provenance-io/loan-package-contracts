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
import tech.figure.util.v1beta1.Checksum
import tech.figure.util.v1beta1.DocumentMetadata

class AddENoteAssumptionContractUnitTest : WordSpec({
    "addAssumption" When {
        val maxExistingAssumptionCount = (if (KotestConfig.runTestsExtended) 20 else 5)
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote()) { randomExistingENote ->
                    shouldThrow<ContractViolationException> {
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomInvalidId
                                assumptionBuilder.checksum = randomChecksum
                                assumptionBuilder.uri = randomUri
                                assumptionBuilder.contentType = randomContentType
                                assumptionBuilder.documentType = randomDocumentType
                                assumptionBuilder.fileName = randomFileName
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomId
                                assumptionBuilder.checksum = Checksum.newBuilder().also { checksumBuilder ->
                                    checksumBuilder.clearChecksum()
                                    checksumBuilder.algorithm = randomChecksumAlgorithm
                                }.build()
                                assumptionBuilder.uri = randomUri
                                assumptionBuilder.contentType = randomContentType
                                assumptionBuilder.documentType = randomDocumentType
                                assumptionBuilder.fileName = randomFileName
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomId
                                assumptionBuilder.checksum = randomChecksum
                                assumptionBuilder.contentType = randomContentType
                                assumptionBuilder.documentType = randomDocumentType
                                assumptionBuilder.fileName = randomFileName
                                assumptionBuilder.clearUri()
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomId
                                assumptionBuilder.checksum = randomChecksum
                                assumptionBuilder.uri = randomUri
                                assumptionBuilder.documentType = randomDocumentType
                                assumptionBuilder.fileName = randomFileName
                                assumptionBuilder.clearContentType()
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomId
                                assumptionBuilder.checksum = randomChecksum
                                assumptionBuilder.contentType = randomContentType
                                assumptionBuilder.uri = randomUri
                                assumptionBuilder.fileName = randomFileName
                                assumptionBuilder.clearDocumentType()
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
                        AddENoteAssumptionContract(
                            existingENote = randomExistingENote,
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = randomId
                                assumptionBuilder.checksum = randomChecksum
                                assumptionBuilder.contentType = randomContentType
                                assumptionBuilder.documentType = randomDocumentType
                                assumptionBuilder.uri = randomUri
                                assumptionBuilder.clearFileName()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Document with ID ${randomId.value} is missing file name"
                    }
                }
            }
        }
        "given an input which attempts to silently change properties of an existing assumption" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 1, max = maxExistingAssumptionCount).flatMap { documentCount ->
                        anyValidENote(assumptionCount = documentCount)
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
                        AddENoteAssumptionContract(
                            existingENote = eNote.toBuilder().also { existingENoteBuilder ->
                                existingENoteBuilder.addAssumption(
                                    DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                        assumptionBuilder.id = oldId
                                        assumptionBuilder.checksum = checksum.toBuilder().also { checksumBuilder ->
                                            checksumBuilder.algorithm = oldAl
                                        }.build()
                                        assumptionBuilder.contentType = oldCType
                                        assumptionBuilder.documentType = oldDType
                                        assumptionBuilder.fileName = randomFileName
                                        assumptionBuilder.uri = oldUri
                                    }.build()
                                )
                            }.build(),
                        ).addAssumption(
                            DocumentMetadata.newBuilder().also { assumptionBuilder ->
                                assumptionBuilder.id = newId
                                assumptionBuilder.checksum = checksum.toBuilder().also { checksumBuilder ->
                                    checksumBuilder.algorithm = newAl
                                }.build()
                                assumptionBuilder.contentType = newCType
                                assumptionBuilder.documentType = newDType
                                assumptionBuilder.uri = newUri
                                assumptionBuilder.fileName = randomFileName
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
        "given an input which duplicates an existing assumption's properties with the exception of the file name" should {
            // TODO: Revisit this behavior in the future
            "be added to the list without removing the existing assumption" {
                checkAll(
                    anyValidENote(assumptionCount = 0),
                    Arb.int(min = 1, max = maxExistingAssumptionCount).flatMap { documentCount ->
                        anyValidDocumentSet(size = documentCount)
                    },
                    anyNonEmptyString,
                ) { randomExistingENote, randomExistingAssumptions, randomFileName ->
                    AddENoteAssumptionContract(
                        existingENote = randomExistingENote.toBuilder().also { existingENoteBuilder ->
                            existingENoteBuilder.addAllAssumption(randomExistingAssumptions)
                        }.build(),
                    ).addAssumption(
                        randomExistingAssumptions.takeLast(1)[0].toBuilder().also { newAssumptionBuilder ->
                            newAssumptionBuilder.fileName = randomFileName
                        }.build()
                    ).let { updatedENote ->
                        updatedENote.assumptionCount shouldBe randomExistingAssumptions.size + 1
                        updatedENote.assumptionList.last().fileName shouldBe randomFileName
                    }
                }
            }
        }
        "given a valid input with a completely unique assumption" should {
            "not throw an exception" {
                checkAll(
                    anyValidENote(assumptionCount = 0),
                    Arb.int(min = 1, max = maxExistingAssumptionCount).flatMap { documentCount ->
                        anyValidDocumentSet(size = documentCount)
                    },
                ) { randomExistingENote, randomAssumptions, ->
                    val (existingAssumptions, newAssumption) = randomAssumptions.breakOffLast()
                    AddENoteAssumptionContract(
                        existingENote = randomExistingENote.toBuilder().also { existingENoteBuilder ->
                            existingENoteBuilder.addAllAssumption(existingAssumptions)
                        }.build(),
                    ).addAssumption(
                        newAssumption
                    ).let { updatedENote ->
                        updatedENote.assumptionCount shouldBe randomAssumptions.size
                        updatedENote.assumptionList.last() shouldBe newAssumption
                    }
                }
            }
        }
    }
})
