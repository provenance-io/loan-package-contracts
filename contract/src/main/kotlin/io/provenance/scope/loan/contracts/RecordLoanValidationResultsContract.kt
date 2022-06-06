package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.LoanScopeInputs
import io.provenance.scope.loan.utility.ContractRequirementType
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.loanValidationResultsValidation
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationResponse

@Participants(roles = [PartyType.OWNER]) // TODO: Change to VALIDATOR once latter is defined
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationResultsContract(
    @Record(LoanScopeFacts.loanValidations) val validationRecord: LoanValidation,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Change to VALIDATOR once latter is defined
    @Record(LoanScopeFacts.loanValidations)
    open fun recordLoanValidationResults(
        @Input(LoanScopeInputs.validationResponse) submission: ValidationResponse
    ): LoanValidation {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE,
            validationRecord.isSet() orError "A validation iteration must already exist for results to be submitted",
        )
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            requireThat(
                submission.requestId.isValid() orError "Response must have valid ID",
            )
            loanValidationResultsValidation(submission.results)
            validationRecord.iterationList.singleOrNull { iteration ->
                iteration.request.requestId == submission.requestId // For now, we won't support letting results arrive before the request
            }.let { maybeIteration ->
                requireThat(
                    if (maybeIteration === null) {
                        false orError "No single validation iteration with a matching request ID exists"
                    } else {
                        (submission.results.resultSetProvider == maybeIteration.request.validatorName) orError
                            "Result set provider does not match what was requested in this validation iteration"
                    }
                )
            }
        }
        return validationRecord.iterationList.indexOfLast { iteration -> // The enforcement above ensures exactly one match
            iteration.request.requestId == submission.requestId
        }.let { index ->
            validationRecord.toBuilder().also { recordBuilder ->
                recordBuilder.setIteration(
                    index,
                    validationRecord.iterationList[index].toBuilder().apply {
                        results = submission.results
                    }.build()
                )
            }.build()
        }
    }
}
