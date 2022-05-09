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
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest

@Participants(roles = [PartyType.OWNER]) // TODO: Add/Change to VALIDATOR?
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationRequestContract(
    @Record(LoanScopeFacts.loanValidations) val validationRecord: LoanValidation,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Add/Change to VALIDATOR?
    @Record(LoanScopeFacts.loanValidations)
    open fun recordLoanValidationRequest(@Input(LoanScopeInputs.validationRequest) submission: ValidationRequest): LoanValidation {
        validateRequirements(VALID_INPUT,
            submission.requestId.isValid()        orError "Request ID is missing",
            submission.effectiveTime.isValid()    orError "Request timestamp is missing",
            submission.snapshotUri.isNotBlank()   orError "Loan snapshot URI is missing",
            submission.validatorName.isNotBlank() orError "Validator name is missing",
            submission.requesterName.isNotBlank() orError "Requester name is missing",
            validationRecord.iterationList.none { iteration ->
                iteration.request.requestId == submission.requestId
            } orError "A validation iteration with the same request ID already exists",
        )
        return validationRecord.toBuilder().also { recordBuilder ->
            recordBuilder.addIteration(
                ValidationIteration.newBuilder().also { iterationBuilder ->
                    iterationBuilder.request = submission
                }.build()
            )
        }.build()
    }
}
