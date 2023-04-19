package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.requestContractWithEmptyExistingRecord
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyFutureTimestamp
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationRequest
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.validation.v1beta2.ValidationRequest

class RecordLoanValidationRequestUnitTest : WordSpec({
    "recordLoanValidationRequest" When {
        "given an empty input to an empty scope" should {
            "throw an appropriate exception" {
                shouldThrow<ContractViolationException> {
                    requestContractWithEmptyExistingRecord.apply {
                        recordLoanValidationRequest(ValidationRequest.getDefaultInstance())
                    }
                }.let { exception ->
                    exception shouldHaveViolationCount 1
                    exception.message shouldContain "Request is not set"
                }
            }
        }
        "given an input to an empty scope with an invalid ID" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRequest, anyInvalidUuid) { randomRequest, randomInvalidId ->
                    shouldThrow<ContractViolationException> {
                        requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                            submission = randomRequest.toBuilder().also { requestBuilder ->
                                requestBuilder.requestId = randomInvalidId
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Request must have valid ID"
                    }
                }
            }
        }
        "given an input to an empty scope with an effective time in the future" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRequest, anyFutureTimestamp) { randomRequest, randomInvalidTimestamp ->
                    shouldThrow<ContractViolationException> {
                        requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                            submission = randomRequest.toBuilder().also { requestBuilder ->
                                requestBuilder.effectiveTime = randomInvalidTimestamp
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Request must have valid effective time"
                    }
                }
            }
        }
        "given an input to an empty scope without a block height" should {
            "not throw an exception" {
                checkAll(anyValidValidationRequest) { randomRequest ->
                    randomRequest.toBuilder().also { requestBuilder ->
                        requestBuilder.clearBlockHeight()
                    }.build().let { request ->
                        requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                            submission = request
                        ).let { resultingRecord ->
                            resultingRecord.iterationList.singleOrNull { iteration ->
                                iteration.request.requestId.value == randomRequest.requestId.value
                            }?.request shouldBe request
                        }
                    }
                }
            }
        }
        "given an input to an empty scope without a validator name" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRequest) { randomRequest ->
                    shouldThrow<ContractViolationException> {
                        requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                            submission = randomRequest.toBuilder().also { requestBuilder ->
                                requestBuilder.clearValidatorName()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Request is missing validator name"
                    }
                }
            }
        }
        "given an input to an empty scope without a requester name" should {
            "throw an appropriate exception" {
                checkAll(anyValidValidationRequest) { randomRequest ->
                    shouldThrow<ContractViolationException> {
                        requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                            submission = randomRequest.toBuilder().also { requestBuilder ->
                                requestBuilder.clearRequesterName()
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1
                        exception.message shouldContain "Request is missing requester name"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyValidValidationRequest) { randomRequest ->
                    requestContractWithEmptyExistingRecord.recordLoanValidationRequest(
                        submission = randomRequest
                    ).let { resultingRecord ->
                        resultingRecord.iterationList.singleOrNull { iteration ->
                            iteration.request.requestId.value == randomRequest.requestId.value
                        }?.request shouldBe randomRequest
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
