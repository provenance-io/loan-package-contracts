package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuidSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanDocumentSet
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.breakOffLast
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.test.toPair
import io.provenance.scope.loan.test.toRecord
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.util.v1beta1.DocumentMetadata
import kotlin.math.max

class AppendLoanDocumentsContractUnitTest : WordSpec({
    "appendDocuments" When {
        val maxDocumentCount = (if (KotestConfig.runTestsExtended) 20 else 5)
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
                        ).appendDocuments(
                            newDocs = LoanDocuments.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
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
                    ).appendDocuments(
                        newDocs = LoanDocuments.newBuilder().also { inputBuilder ->
                            inputBuilder.clearDocument()
                            inputBuilder.addDocument(DocumentMetadata.getDefaultInstance())
                        }.build()
                    )
                }.let { exception ->
                    exception shouldHaveViolationCount 1U
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
                        exception shouldHaveViolationCount 1U
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
                ) { randomDocuments, (oldId, newId), (oldUri, newUri), (oldContentType, newContentType), (oldDocumentType, newDocumentType) ->
                    val (existingDocuments, newDocument) = randomDocuments.documentList.breakOffLast()
                    shouldThrow<ContractViolationException> {
                        AppendLoanDocumentsContract(
                            existingDocs = existingDocuments.toRecord().toBuilder().also { recordBuilder ->
                                (recordBuilder.documentCount - 1).let { indexOfDocumentToModify ->
                                    recordBuilder.setDocument(
                                        indexOfDocumentToModify,
                                        newDocument.toBuilder().also { documentBuilder ->
                                            documentBuilder.id = oldId
                                            documentBuilder.uri = oldUri
                                            documentBuilder.contentType = oldContentType
                                            documentBuilder.documentType = oldDocumentType
                                        }.build()
                                    )
                                }
                            }.build(),
                        ).appendDocuments(
                            newDocs = LoanDocuments.newBuilder().also { inputBuilder ->
                                inputBuilder.clearDocument()
                                inputBuilder.addDocument(
                                    newDocument.toBuilder().also { documentBuilder ->
                                        documentBuilder.id = newId
                                        documentBuilder.uri = newUri
                                        documentBuilder.contentType = newContentType
                                        documentBuilder.documentType = newDocumentType
                                    }.build()
                                )
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 4U
                        listOf("ID", "URI", "content type", "document type").forEach { immutableField ->
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
                            Arb.int(0..max(randomDocumentCount - 1, 1)),
                        )
                    }
                ) { (randomDocuments, randomSplit) ->
                    val (randomExistingDocuments, randomNewDocuments) = randomDocuments.documentList.let { randomDocumentSet ->
                        randomDocumentSet.take(randomSplit) to randomDocumentSet.drop(randomSplit)
                    }
                    AppendLoanDocumentsContract(
                        existingDocs = randomExistingDocuments.toRecord(),
                    ).appendDocuments(
                        newDocs = randomNewDocuments.toRecord()
                    )
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
