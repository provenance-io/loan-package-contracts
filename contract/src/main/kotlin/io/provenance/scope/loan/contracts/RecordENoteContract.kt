package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.eNoteValidation
import io.provenance.scope.loan.utility.validateRequirements

@Participants(roles = [Specifications.PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class RecordENoteContract : P8eContract() {
    // TODO: Add function modifying servicing data record with servicing data as input
    @Function(invokedBy = Specifications.PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun recordENote(@Input(LoanScopeFacts.eNote) eNote: ENote) = eNote.also {
        validateRequirements(VALID_INPUT) {
            eNoteValidation(eNote)
        }
    }
}
