package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.provenance.scope.loan.test.Constructors.contractWithEmptyExistingValidationRecord
import io.provenance.scope.loan.test.Constructors.contractWithSingleValidationIteration
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.loan.test.Constructors.validResultSubmission
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.IllegalContractStateException
import tech.figure.validation.v1beta1.ValidationResponse

class RecordLoanValidationResultsUnitTest : WordSpec({
    "recordLoanValidationResults" When {
        "executed without a validation request existing in the scope" should {
            "throw an appropriate exception" {
                shouldThrow<IllegalContractStateException> {
                    contractWithEmptyExistingValidationRecord.apply {
                        recordLoanValidationResults(validResultSubmission(randomProtoUuid))
                    }
                }.let { exception ->
                    exception.message shouldContainIgnoringCase "validation iteration must exist"
                }
            }
        }
        "given an invalid input" should {
            "throw an appropriate exception" {
                ValidationResponse.getDefaultInstance().let { emptyResultSubmission ->
                    shouldThrow<ContractViolationException> {
                        contractWithSingleValidationIteration(randomProtoUuid).apply {
                            recordLoanValidationResults(emptyResultSubmission)
                        }
                    }.let { exception ->
                        exception.message shouldContain "Result set UUID is missing"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                shouldNotThrow<ContractViolationException> {
                    randomProtoUuid.let { iterationRequestId ->
                        contractWithSingleValidationIteration(
                            requestID = iterationRequestId,
                            validatorName = "My Favorite Provider",
                        ).apply {
                            recordLoanValidationResults(
                                validResultSubmission(
                                    iterationRequestID = iterationRequestId,
                                    resultSetProvider = "My Favorite Provider",
                                )
                            )
                        }
                    }
                }
            }
        }
    }
})
