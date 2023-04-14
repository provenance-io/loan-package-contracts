package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.standaloneResultsContractWithEmptyExistingRecord
import io.provenance.scope.loan.test.KotestConfig
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationIteration
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationRecord
import io.provenance.scope.loan.test.breakOffLast
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.validation.v1beta2.ValidationIteration

class RecordStandaloneLoanValidationResultsUnitTest : WordSpec({
    "recordStandaloneLoanValidationResults" When {
        "executed without a validation request existing in the scope" should {
            "not throw an exception" {
                checkAll(anyValidValidationIteration) { randomValidationIteration ->
                    standaloneResultsContractWithEmptyExistingRecord.recordStandaloneLoanValidationResults(
                        submission = randomValidationIteration,
                    ).let { resultingRecord ->
                        resultingRecord.iterationList.singleOrNull() shouldBe randomValidationIteration
                    }
                }
            }
        }
        val anyValidValidationRecord = Arb.int(min = 1, max = (if (KotestConfig.runTestsExtended) 15 else 5)).flatMap { iterationCount ->
            anyValidValidationRecord(iterationCount = iterationCount)
        }
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRecord.orNull()) { randomExistingRecord ->
                    shouldThrow<ContractViolationException> {
                        RecordStandaloneLoanValidationResultsContract(
                            validationRecord = randomExistingRecord,
                        ).recordStandaloneLoanValidationResults(
                            submission = ValidationIteration.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 2
                        exception.message shouldContain "Request is not set"
                        exception.message shouldContain "Results are not set"
                    }
                }
            }
        }
        "given an input without a valid result ID" should {
            "throw an appropriate exception" {
                checkAll(
                    anyValidValidationRecord.orNull(),
                    anyValidValidationIteration,
                    anyInvalidUuid,
                ) { randomExistingRecord, randomSubmission, randomInvalidId ->
                    shouldThrow<ContractViolationException> {
                        RecordStandaloneLoanValidationResultsContract(
                            validationRecord = randomExistingRecord,
                        ).recordStandaloneLoanValidationResults(
                            submission = randomSubmission.toBuilder().also { submissionBuilder ->
                                submissionBuilder.results = submissionBuilder.results.toBuilder().also { resultsBuilder ->
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
            "not throw an exception" {
                checkAll(
                    Arb.int(min = 2, max = (if (KotestConfig.runTestsExtended) 15 else 5)).flatMap { iterationCount ->
                        anyValidValidationRecord(iterationCount = iterationCount)
                    },
                ) { randomRecord ->
                    val (existingIterations, otherIterations) = randomRecord.iterationList.breakOffLast(2)
                    val (reservedIteration, randomNewIteration) = otherIterations
                    RecordStandaloneLoanValidationResultsContract(
                        validationRecord = randomRecord.toBuilder().also { recordBuilder ->
                            recordBuilder.clearIteration()
                            recordBuilder.addAllIteration(existingIterations)
                            recordBuilder.addIteration(
                                randomNewIteration.toBuilder().also { iterationBuilder ->
                                    iterationBuilder.clearResults()
                                }.build()
                            )
                        }.build(),
                    ).recordStandaloneLoanValidationResults(
                        submission = randomNewIteration
                    ).let { resultingRecord ->
                        resultingRecord.iterationList.lastOrNull { iteration ->
                            iteration.results.id.value == randomNewIteration.results.id.value
                        } shouldBe randomNewIteration
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyValidValidationRecord, anyValidValidationIteration) { randomRecord, randomSubmission ->
                    RecordStandaloneLoanValidationResultsContract(
                        validationRecord = randomRecord,
                    ).recordStandaloneLoanValidationResults(
                        submission = randomSubmission
                    ).let { resultingRecord ->
                        resultingRecord.iterationList.lastOrNull { iteration ->
                            iteration.results.id.value == randomSubmission.results.id.value
                        } shouldBe randomSubmission
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
