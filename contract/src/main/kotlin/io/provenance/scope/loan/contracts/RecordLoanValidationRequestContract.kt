package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationRequest

@Participants(roles = [PartyType.OWNER]) // TODO: Add/Change to VALIDATOR?
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationRequestContract(
    @Record(LoanScopeFacts.loanValidations) val existingResults: LoanValidation,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Add/Change to VALIDATOR?
    @Record(LoanScopeFacts.loanValidations)
    open fun recordLoanValidationRequest(@Input(name = "newRequest") newRequest: ValidationRequest) { // TODO: Change signature & annotations
    }
}
