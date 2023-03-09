package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.DocumentRecordingGuidance
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.provenance.scope.loan.LoanScopeProperties.servicingDocumentsKey
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuidSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENoteController
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingData
import io.provenance.scope.loan.test.MetadataAssetModelArbs.loanStateSet
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.breakOffLast
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.toPair
import io.provenance.scope.loan.test.toRecord
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.proto.util.toProtoAny
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.util.v1beta1.DocumentMetadata
import kotlin.math.max

class AppendLoanDocumentsContractUnitTest : WordSpec({
    "appendDocuments" When {
        val maxDocumentCount = (if (KotestConfig.runTestsExtended) 15 else 5)
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 0, max = maxDocumentCount).flatMap { existingDocumentCount ->
                        anyValidLoanDocumentSet(size = existingDocumentCount)
                    },
                ) { randomExistingDocuments ->
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingDocs = randomExistingDocuments,
                            existingServicingData = ServicingData.getDefaultInstance(), // Unused
                        ).appendDocuments(
                            newDocs = LoanDocuments.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Must supply at least one document"
                    }
                }
            }
        }
        "given an input to an empty scope with at least one invalid document" should {
            "throw an appropriate exception" {
                shouldThrow<ContractViolationException> {
                    AppendLoanDocumentsContract(
                        existingDocs = LoanDocuments.getDefaultInstance(),
                        existingServicingData = ServicingData.getDefaultInstance(), // Unused
                    ).appendDocuments(
                        newDocs = LoanDocuments.newBuilder().also { inputBuilder ->
                            inputBuilder.clearDocument()
                            inputBuilder.addDocument(DocumentMetadata.getDefaultInstance())
                        }.build()
                    )
                }.let { exception ->
                    exception shouldHaveViolationCount 1
                    exception.message shouldContain "Document is not set"
                }
            }
        }
        "given an input with at least one invalid document to append to existing documents in the scope" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 1, max = maxDocumentCount).flatMap { existingDocumentCount ->
                        anyValidLoanDocumentSet(size = existingDocumentCount)
                    },
                    anyInvalidUuid,
                ) { randomDocuments, randomInvalidId ->
                    val (existingDocuments, newDocument) = randomDocuments.documentList.breakOffLast()
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingDocs = existingDocuments.toRecord(),
                            existingServicingData = ServicingData.getDefaultInstance(), // Unused
                        ).appendDocuments(
                            newDocs = LoanDocuments.newBuilder().also { inputBuilder ->
                                inputBuilder.clearDocument()
                                inputBuilder.addDocument(
                                    newDocument.toBuilder().also { documentBuilder ->
                                        documentBuilder.id = randomInvalidId
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
        "given an input which attempts to silently change properties of an existing document" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 2, max = maxDocumentCount).flatMap { documentCount ->
                        anyValidLoanDocumentSet(size = documentCount)
                    },
                    anyUuidSet(size = 2).toPair(),
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                    Arb.set(gen = anyNonEmptyString, size = 2).toPair { set -> set.toList() },
                ) { randomDocs, (oldId, newId), (oldAlgo, newAlgo), (oldUri, newUri), (oldContentType, newContentType), (oldDocType, newDocType) ->
                    val (existingDocuments, newDocument) = randomDocs.documentList.breakOffLast()
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingDocs = existingDocuments.toRecord().toBuilder().also { recordBuilder ->
                                (recordBuilder.documentCount - 1).let { indexOfDocumentToModify ->
                                    recordBuilder.setDocument(
                                        indexOfDocumentToModify,
                                        newDocument.toBuilder().also { documentBuilder ->
                                            documentBuilder.checksum = newDocument.checksum.toBuilder().also { checksumBuilder ->
                                                checksumBuilder.algorithm = oldAlgo
                                            }.build()
                                            documentBuilder.id = oldId
                                            documentBuilder.uri = oldUri
                                            documentBuilder.contentType = oldContentType
                                            documentBuilder.documentType = oldDocType
                                        }.build()
                                    )
                                }
                            }.build(),
                            existingServicingData = ServicingData.getDefaultInstance(), // Unused
                        ).appendDocuments(
                            newDocs = LoanDocuments.newBuilder().also { inputBuilder ->
                                inputBuilder.clearDocument()
                                inputBuilder.addDocument(
                                    newDocument.toBuilder().also { documentBuilder ->
                                        documentBuilder.checksum = newDocument.checksum.toBuilder().also { checksumBuilder ->
                                            checksumBuilder.algorithm = newAlgo
                                        }.build()
                                        documentBuilder.id = newId
                                        documentBuilder.uri = newUri
                                        documentBuilder.contentType = newContentType
                                        documentBuilder.documentType = newDocType
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 5
                        listOf("checksum algorithm", "ID", "URI", "content type", "document type").forEach { immutableField ->
                            exception.message shouldContain
                                "Cannot change $immutableField of existing document with checksum ${newDocument.checksum.checksum}"
                        }
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                val documentCountRange = 2..(if (KotestConfig.runTestsExtended) 8 else 3)
                checkAll(
                    Arb.int(documentCountRange).flatMap { randomDocumentCount ->
                        Arb.pair(
                            anyValidLoanDocumentSet(size = randomDocumentCount),
                            Arb.int(1..max(randomDocumentCount - 1, 1)),
                        )
                    }
                ) { (randomDocuments, randomSplit) ->
                    val (randomExistingDocuments, randomNewDocuments) = randomDocuments.documentList.breakOffLast(randomSplit)
                    AppendLoanDocumentsContract(
                        existingDocs = randomExistingDocuments.toRecord(),
                        existingServicingData = ServicingData.getDefaultInstance(), // Unused
                    ).appendDocuments(
                        newDocs = randomNewDocuments.toRecord()
                    )
                }
            }
        }
    }
    "appendServicingDocuments" When {
        "given an empty input" should {
            "do nothing to the existing servicing data" {
                checkAll(
                    anyValidServicingData(loanStateAndDocumentCount = 6),
                ) { randomExistingServicingData ->
                    AppendLoanDocumentsContract(
                        existingServicingData = randomExistingServicingData,
                        existingDocs = LoanDocuments.getDefaultInstance(), // Unused
                    ).appendServicingDocuments(
                        newDocs = LoanDocuments.getDefaultInstance()
                    ).let { result ->
                        result shouldBe randomExistingServicingData
                    }
                }
            }
        }
        "given an input with a servicing documents value not of the expected type" should {
            "throw an appropriate exception" {
                val documentCountRange = 2..(if (KotestConfig.runTestsExtended) 8 else 4)
                checkAll(
                    Arb.int(documentCountRange).flatMap { randomDocumentCount ->
                        Arb.pair(
                            anyValidLoanDocumentSet(size = randomDocumentCount),
                            Arb.int(1..randomDocumentCount),
                        )
                    },
                    anyValidServicingData(loanStateAndDocumentCount = 6),
                    anyValidENoteController,
                ) { (randomDocuments, newDocumentCount), randomServicingData, randomInvalidType ->
                    val (randomExistingDocuments, randomNewDocuments) = randomDocuments.documentList.breakOffLast(newDocumentCount)
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingServicingData = randomServicingData,
                            existingDocs = randomExistingDocuments.toRecord(),
                        ).appendServicingDocuments(
                            LoanDocuments.newBuilder().also { documentsBuilder ->
                                documentsBuilder.addAllDocument(randomNewDocuments)
                                documentsBuilder.putMetadataKv(
                                    servicingDocumentsKey,
                                    randomInvalidType.toProtoAny()
                                )
                            }.build(),
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain
                            "Could not unpack the input's \"${servicingDocumentsKey}\" metadata as ${DocumentRecordingGuidance::class.java}"
                    }
                }
            }
        }
        "given an input which attempts to silently change the URI of an existing document" should {
            "throw an appropriate exception" {
                val documentCountRange = 2..(if (KotestConfig.runTestsExtended) 8 else 3)
                val loanStateCountRange = 2..(if (KotestConfig.runTestsExtended) 8 else 3)
                checkAll(
                    Arb.int(documentCountRange).flatMap { randomDocumentCount ->
                        anyValidDocumentSet(size = randomDocumentCount, slippage = 10)
                    },
                    Arb.int(loanStateCountRange).flatMap { randomLoanStateCount ->
                        loanStateSet(size = randomLoanStateCount, slippage = 10)
                    },
                ) { randomDocuments, randomLoanStates ->
                    val (randomExistingDocuments, unusedDocument) = randomDocuments.breakOffLast()
                    val duplicateDocument = randomExistingDocuments.last()
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                                servicingDataBuilder.addAllLoanState(randomLoanStates)
                                servicingDataBuilder.addAllDocMeta(randomExistingDocuments)
                            }.build(),
                            existingDocs = LoanDocuments.newBuilder().also { documentsBuilder ->
                                documentsBuilder.addAllDocument(randomExistingDocuments)
                            }.build(),
                        ).appendServicingDocuments(
                            newDocs = LoanDocuments.newBuilder().also { documentsBuilder ->
                                documentsBuilder.addDocument(
                                    duplicateDocument.toBuilder().also { duplicateDocumentBuilder ->
                                        duplicateDocumentBuilder.uri = unusedDocument.uri // Generate an extra document to get a non-duplicate URI
                                    }.build()
                                )
                                documentsBuilder.putMetadataKv(
                                    servicingDocumentsKey,
                                    DocumentRecordingGuidance.newBuilder().also { guidanceBuilder ->
                                        guidanceBuilder.putDesignatedDocuments(
                                            duplicateDocument.id.value,
                                            true
                                        )
                                    }.build().toProtoAny()
                                )
                            }.build(),
                        )
                    }.let { exception ->
                        exception.message shouldContain "Cannot change URI of existing document with checksum ${duplicateDocument.checksum.checksum}"
                    }
                }
            }
        }
        "given a valid input with at least one new servicing document" should {
            "not throw an exception" {
                val documentCountRange = 1..(if (KotestConfig.runTestsExtended) 8 else 4)
                val loanStateCountRange = 1..(if (KotestConfig.runTestsExtended) 8 else 4)
                checkAll(
                    Arb.int(documentCountRange).flatMap { randomDocumentCount ->
                        Arb.pair(
                            anyValidLoanDocumentSet(size = randomDocumentCount),
                            Arb.int(0 until randomDocumentCount).flatMap { randomExistingDocumentCount ->
                                Arb.pair(
                                    Arb.int(0..randomExistingDocumentCount).map { randomExistingServicingDocumentCount ->
                                        Pair(randomExistingDocumentCount, randomExistingServicingDocumentCount)
                                    },
                                    Arb.int(1..(randomDocumentCount - randomExistingDocumentCount)),
                                )
                            },
                        )
                    },
                    Arb.int(loanStateCountRange).flatMap { randomLoanStateCount ->
                        loanStateSet(size = randomLoanStateCount, slippage = 10)
                    },
                ) { (randomDocuments, randomPartitionData), randomLoanStates ->
                    val (existingDocumentSplits, newServicingDocumentCount) = randomPartitionData
                    val (existingLoanDocumentSplit, existingServicingDocumentSplit) = existingDocumentSplits
                    val (
                        randomExistingServicingDocuments,
                        randomExistingLoanDocuments,
                        randomNewServicingDocuments,
                        randomNewLoanDocuments,
                    ) = randomDocuments.documentList.run {
                        listOf(
                            subList(0, existingServicingDocumentSplit),
                            subList(0, existingLoanDocumentSplit),
                            subList(existingLoanDocumentSplit, existingLoanDocumentSplit + newServicingDocumentCount),
                            subList(existingLoanDocumentSplit, size),
                        )
                    }
                    AppendLoanDocumentsContract(
                        existingDocs = LoanDocuments.newBuilder().also { documentsBuilder ->
                            documentsBuilder.addAllDocument(randomExistingLoanDocuments)
                        }.build(),
                        existingServicingData = ServicingData.newBuilder().also { servicingDataBuilder ->
                            servicingDataBuilder.addAllLoanState(randomLoanStates)
                            servicingDataBuilder.addAllDocMeta(randomExistingServicingDocuments)
                        }.build(),
                    ).appendServicingDocuments(
                        newDocs = LoanDocuments.newBuilder().also { documentsBuilder ->
                            documentsBuilder.addAllDocument(randomNewLoanDocuments)
                            documentsBuilder.putMetadataKv(
                                servicingDocumentsKey,
                                DocumentRecordingGuidance.newBuilder().also { guidanceBuilder ->
                                    guidanceBuilder.putAllDesignatedDocuments(
                                        randomNewServicingDocuments.associate { document -> document.id.value to true }
                                    )
                                }.build().toProtoAny()
                            )
                        }.build(),
                    ).let { result ->
                        result.loanStateList shouldContainExactlyInAnyOrder randomLoanStates
                        result.docMetaCount shouldBeExactly randomExistingServicingDocuments.size + randomNewServicingDocuments.size
                        result.docMetaList shouldContainExactlyInAnyOrder randomExistingServicingDocuments + randomNewServicingDocuments
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
