package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.Controller
import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements

@Participants([PartyType.OWNER, PartyType.CUSTODIAN])
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteControllerContract(
    @Record(LoanScopeFacts.eNote) val existingENote: ENote?, // TODO: Confirm if this should be nullable and adjust code below accordingly
) : P8eContract() {
    @Function(invokedBy = PartyType.CUSTODIAN) // TODO: Replace OWNER with CONTROLLER
    @Record(LoanScopeFacts.eNote)
    open fun updateENoteController(@Input(name = "newController") newController: Controller): ENote {
        validateRequirements(
            (existingENote !== null)                  orError "Cannot create eNote using this contract",
            newController.controllerUuid.isValid()    orError "Controller UUID is missing",
            newController.controllerName.isNotBlank() orError "Controller Name is missing",
        )
        return existingENote!!.toBuilder().setController(newController).build()
    }
}
