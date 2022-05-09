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
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements

@Participants([PartyType.OWNER/*, PartyType.CONTROLLER*/]) // TODO: Add controller or ensure Authz grant to controller is made
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteControllerContract(
    @Record(LoanScopeFacts.eNote) val existingENote: ENote,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER/*PartyType.CONTROLLER*/) // TODO: Change to controller or ensure Authz grant to controller is made
    @Record(LoanScopeFacts.eNote)
    open fun updateENoteController(@Input(LoanScopeInputs.eNoteControllerUpdate) newController: Controller): ENote {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE,
            existingENote.isSet() orError "Cannot create eNote using this contract",
        )
        validateRequirements(ContractRequirementType.VALID_INPUT,
            newController.controllerUuid.isValid()    orError "Controller UUID is missing",
            newController.controllerName.isNotBlank() orError "Controller Name is missing",
        )
        return existingENote.toBuilder().setController(newController).build()
    }
}
