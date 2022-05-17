package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.Controller
import io.dartinc.registry.v1beta1.ENote
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
import io.provenance.scope.loan.utility.eNoteControllerValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements

@Participants([PartyType.OWNER, PartyType.CUSTODIAN]) // TODO: Change temporary placeholder of "CUSTODIAN" to "CONTROLLER" once latter is defined
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteControllerContract(
    @Record(LoanScopeFacts.eNote) val existingENote: ENote,
) : P8eContract() {

    @Function(invokedBy = PartyType.CUSTODIAN) // TODO: Change temporary placeholder of "CUSTODIAN" to "CONTROLLER" once latter is defined
    @Record(LoanScopeFacts.eNote)
    open fun updateENoteController(@Input(LoanScopeInputs.eNoteControllerUpdate) newController: Controller): ENote {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE,
            existingENote.isSet() orError "Cannot create eNote using this contract",
        )
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            eNoteControllerValidation(newController)
        }
        return existingENote.toBuilder().setController(newController).build()
    }
}
