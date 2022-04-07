package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants(CONTROLLER)
@ScopeSpecification(["tech.figure.loan"])
open class UpdateENoteContract(
    @Record(LoanScopeFacts.eNote) val eNote: io.dartinc.registry.v1beta1.ENote,
) : P8eContract() {

    @Function(CONTROLLER)
    @Record(LoanScopeFacts.eNote)
    open fun updateENote(@Input(name = "newENote") newENote: tech.figure.util.v1beta1.DocumentMetadata) {
        require(existingENote != null) { "Cannot create eNote using this contract" }
        require(newENote.id.isValid()) { "ENote missing ID" }
        require(newENote.uri.isNotBlank()) { "ENote missing uri" }
        require(newENote.content_type.isNotBlank()) { "ENote missing content type" }
        require(newENote.document_type.isNotBlank()) { "ENote missing document type" }
        require(newENote.checksum.isNotBlank()) { "ENote missing checksum" }
        return existingENote.toBuilder().setENote(newENote).build()
    }

}