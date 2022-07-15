package io.provenance.scope.loan.contracts

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
import io.provenance.scope.loan.utility.documentModificationValidation
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.toChecksumMap
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.util.v1beta1.DocumentMetadata

@Participants(roles = [PartyType.OWNER]) // To be invoked by either the servicer or sub-servicer
@ScopeSpecification(["tech.figure.loan"])
open class AddENoteModificationContract(
    @Record(name = LoanScopeFacts.eNote, optional = false) val existingENote: ENote,
) : P8eContract() {

    @Suppress("Duplicates") /** Identical to [AddENoteAssumptionContract.addAssumption]. */
    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.eNote)
    open fun addModification(@Input(LoanScopeInputs.newModification) newModification: DocumentMetadata): ENote {
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            documentValidation(newModification)
            /* Primitive type used for protobuf keys to avoid comparison interference from unknown fields */
            val existingModificationDocumentMetadata = existingENote.modificationList.toChecksumMap()
            newModification.checksum.checksum.takeIf { it.isNotBlank() }?.let { newModificationChecksum ->
                existingModificationDocumentMetadata[newModificationChecksum]?.let { existingModification ->
                    documentModificationValidation(
                        existingModification,
                        newModification,
                    )
                }
            }
        }
        return existingENote.toBuilder().addModification(newModification).build()
    }
}
