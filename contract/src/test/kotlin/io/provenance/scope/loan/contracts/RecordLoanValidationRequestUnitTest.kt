package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.string.shouldContain
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.loan.test.Constructors.requestContractWithEmptyExistingRecord
import io.provenance.scope.loan.test.Constructors.validRequest
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.validation.v1beta1.ValidationRequest

class RecordLoanValidationRequestUnitTest : WordSpec({
    "recordLoanValidationRequest" When {
        "given an invalid input" should {
            "throw an appropriate exception" {
                ValidationRequest.getDefaultInstance().let { emptyResultSubmission ->
                    shouldThrow<ContractViolationException> {
                        requestContractWithEmptyExistingRecord.apply {
                            recordLoanValidationRequest(emptyResultSubmission)
                        }
                    }.let { exception ->
                        exception.message shouldContain "Request ID is missing"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                shouldNotThrow<ContractViolationException> {
                    requestContractWithEmptyExistingRecord.apply {
                        recordLoanValidationRequest(
                            validRequest(
                                requestID = randomProtoUuid,
                                validatorName = "My Adequate Provider",
                            )
                        )
                    }
                }
            }
        }
    }
})
