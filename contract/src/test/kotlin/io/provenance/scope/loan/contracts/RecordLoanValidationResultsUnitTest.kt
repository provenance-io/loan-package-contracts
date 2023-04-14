package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.resultsContractWithEmptyExistingRecord
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationRecord
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationResponse
import io.provenance.scope.loan.test.breakOffLast
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.IllegalContractStateException
import tech.figure.validation.v1beta2.LoanValidation
import tech.figure.validation.v1beta2.ValidationResponse

class RecordLoanValidationResultsUnitTest : WordSpec({
    "recordLoanValidationResults" When {
        "executed without a validation request existing in the scope" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationResponse) { randomValidationResponse ->
                    shouldThrow<IllegalContractStateException> {
                        resultsContractWithEmptyExistingRecord.recordLoanValidationResults(
                            submission = randomValidationResponse,
                        )
                    }.let { exception ->
                        exception.message shouldContainIgnoringCase "validation iteration must already exist"
                    }
                }
            }
        }
        val anyValidValidationRecord = Arb.int(min = 1, max = (if (KotestConfig.runTestsExtended) 15 else 5)).flatMap { iterationCount ->
            anyValidValidationRecord(iterationCount = iterationCount)
        }
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRecord) { randomExistingRecord ->
                    shouldThrow<ContractViolationException> {
                        RecordLoanValidationResultsContract(
                            validationRecord = randomExistingRecord,
                        ).recordLoanValidationResults(
                            submission = ValidationResponse.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception.message shouldContain "Results are not set"
                    }
                }
            }
        }
        "given an input without a valid result ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidValidationRecord,
                    anyInvalidUuid,
                ) { randomRecord, randomInvalidId ->
                    val (existingIterations, randomNewIteration) = randomRecord.iterationList.breakOffLast()
                    shouldThrow<ContractViolationException> {
                        RecordLoanValidationResultsContract(
                            validationRecord = randomRecord.toBuilder().also { recordBuilder ->
                                recordBuilder.clearIteration()
                                recordBuilder.addAllIteration(existingIterations)
                                recordBuilder.addIteration(
                                    randomNewIteration.toBuilder().also { iterationBuilder ->
                                        iterationBuilder.clearResults()
                                    }.build()
                                )
                            }.build(),
                        ).recordLoanValidationResults(
                            submission = ValidationResponse.newBuilder().also { responseBuilder ->
                                responseBuilder.requestId = randomNewIteration.request.requestId
                                responseBuilder.results = randomNewIteration.results.toBuilder().also { resultsBuilder ->
                                    resultsBuilder.id = randomInvalidId
                                }.build()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Results must have valid ID"
                    }
                }
            }
        }
        "given an input which does not complete an existing validation request" should {
            "throw an appropriate exception" {
                checkAll(
                    Arb.int(min = 2, max = (if (KotestConfig.runTestsExtended) 15 else 5)).flatMap { iterationCount ->
                        anyValidValidationRecord(iterationCount = iterationCount)
                    },
                ) { randomRecord ->
                    val (existingIterations, otherIterations) = randomRecord.iterationList.breakOffLast(2)
                    val (reservedIteration, randomNewIteration) = otherIterations
                    shouldThrow<ContractViolationException> {
                        RecordLoanValidationResultsContract(
                            validationRecord = randomRecord.toBuilder().also { recordBuilder ->
                                recordBuilder.clearIteration()
                                recordBuilder.addAllIteration(existingIterations)
                                recordBuilder.addIteration(
                                    randomNewIteration.toBuilder().also { iterationBuilder ->
                                        iterationBuilder.clearResults()
                                    }.build()
                                )
                            }.build(),
                        ).recordLoanValidationResults(
                            submission = ValidationResponse.newBuilder().also { responseBuilder ->
                                responseBuilder.requestId = reservedIteration.request.requestId
                                responseBuilder.results = randomNewIteration.results
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "No single validation iteration with a matching request ID exists"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyValidValidationRecord) { randomRecord ->
                    val (existingIterations, randomNewIteration) = randomRecord.iterationList.breakOffLast()
                    RecordLoanValidationResultsContract(
                        validationRecord = LoanValidation.newBuilder().also { recordBuilder ->
                            recordBuilder.clearIteration()
                            recordBuilder.addAllIteration(existingIterations)
                            recordBuilder.addIteration(
                                randomNewIteration.toBuilder().also { iterationBuilder ->
                                    iterationBuilder.clearResults()
                                }.build()
                            )
                        }.build(),
                    ).recordLoanValidationResults(
                        submission = ValidationResponse.newBuilder().also { responseBuilder ->
                            responseBuilder.requestId = randomNewIteration.request.requestId
                            responseBuilder.results = randomNewIteration.results
                        }.build()
                    ).let { resultingRecord ->
                        resultingRecord.iterationList.singleOrNull { iteration ->
                            iteration.request.requestId.value == randomNewIteration.request.requestId.value
                        } shouldBe randomNewIteration
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
