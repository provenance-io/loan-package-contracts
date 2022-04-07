package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordLoanValidationRequestContract(
    @Record(LoanScopeFacts.validationResults) val existingResults: tech.figure.validation.v1beta1.Validation,
) : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.validationResults)
    open fun recordLoanValidationRequest(@Input(name = "newRequest") newRequest: tech.figure.validation.v1beta1.ValidationRequest) {

    }

}