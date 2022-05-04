package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.string.shouldContain
import io.provenance.scope.loan.test.Constructors.contractWithEmptyExistingValidationRecord
import io.provenance.scope.loan.test.Constructors.contractWithSingleValidationIteration
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.loan.test.Constructors.validResultSubmission
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.validation.v1beta1.ValidationResponse

class RecordLoanValidationResultsUnitTest : WordSpec({
    "recordLoanValidationResults" When {
        "given an invalid input" should {
            "throw an appropriate exception" {
                ValidationResponse.getDefaultInstance().let { emptyResultSubmission ->
                    shouldThrow<ContractViolationException> {
                        contractWithEmptyExistingValidationRecord.apply {
                            recordLoanValidationResults(emptyResultSubmission)
                        }
                    }.let { exception ->
                        exception.message shouldContain "Result set UUID is missing"
                    }
                }
            }
        }
        "given a valid input" xshould { // TODO: Enable once validResultSubmission is implemented
            "not throw an exception" {
                shouldNotThrow<ContractViolationException> {
                    randomProtoUuid.let { iterationRequestId ->
                        contractWithSingleValidationIteration(iterationRequestId).apply {
                            recordLoanValidationResults(validResultSubmission(iterationRequestId))
                        }
                    }
                }
            }
        }
    }
})
