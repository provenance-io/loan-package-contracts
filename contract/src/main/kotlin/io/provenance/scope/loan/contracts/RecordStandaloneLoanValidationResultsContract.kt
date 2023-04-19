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
import io.provenance.scope.loan.utility.loanValidationRequestValidation
import io.provenance.scope.loan.utility.loanValidationResultsValidation
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.validation.v1beta2.LoanValidation
import tech.figure.validation.v1beta2.ValidationIteration

@Participants(roles = [PartyType.VALIDATOR])
@ScopeSpecification(["tech.figure.loan"])
open class RecordStandaloneLoanValidationResultsContract(
    @Record(name = LoanScopeFacts.loanValidationMetadata, optional = true) val validationRecord: LoanValidation?,
) : P8eContract() {

    @Function(invokedBy = PartyType.VALIDATOR)
    @Record(LoanScopeFacts.loanValidationMetadata)
    open fun recordStandaloneLoanValidationResults(
        @Input(LoanScopeInputs.validationIteration) submission: ValidationIteration
    ): LoanValidation {
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            loanValidationRequestValidation(submission.request)
            loanValidationResultsValidation(submission.results)
        }
        return (validationRecord?.toBuilder() ?: LoanValidation.newBuilder()).also { recordBuilder ->
            recordBuilder.addIteration(submission)
        }.build()
    }
}
