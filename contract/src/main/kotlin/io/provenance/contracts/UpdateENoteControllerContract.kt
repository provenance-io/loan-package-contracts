package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER, CONTROLLER])
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteControllerContract(
    @Record(LoanScopeFacts.eNote) val eNote: io.dartinc.registry.v1beta1.ENote,
) : P8eContract() {

    @Function(CONTROLLER)
    @Record(LoanScopeFacts.eNote)
    open fun updateENoteController(@Input(name = "newController") newController: io.dartinc.registry.v1beta1.Controller) {
        require(existingENote != null) { "Cannot create eNote using this contract" }
        require(newController.controllerUuid.isValid()) { "Controller UUID is missing" }
        require(newController.controllerName.isNotBlank()) { "Controller Name is missing" }
        return existingENote.toBuilder().setController(newController).build()
    }

}