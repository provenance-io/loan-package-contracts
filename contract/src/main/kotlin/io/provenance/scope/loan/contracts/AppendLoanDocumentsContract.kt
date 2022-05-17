package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.loan.v1beta1.LoanDocuments

@Participants(roles = [PartyType.OWNER]) // TODO: Should this eventually be changed to SERVICER?
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanDocumentsContract(
    @Record(LoanScopeFacts.documents) val existingDocs: LoanDocuments,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER) // TODO: Should this eventually be changed to SERVICER?
    @Record(LoanScopeFacts.documents)
    open fun appendDocuments(@Input(LoanScopeFacts.documents) newDocs: LoanDocuments): LoanDocuments {
        val newDocList = LoanDocuments.newBuilder().mergeFrom(existingDocs)
        validateRequirements(VALID_INPUT) {
            /* Primitive type used for protobuf keys to avoid comparison interference from unknown fields */
            val existingDocChecksums = existingDocs.documentList.fold(mutableMapOf<String, Boolean>()) { acc, documentMetadata ->
                acc.apply {
                    documentMetadata.checksum.takeIf { it.isValid() }?.checksum?.let { checksum ->
                        acc[checksum] = true
                    }
                }
            }
            requireThat(
                newDocs.documentList.isNotEmpty() orError "Must supply at least one document"
            )
            newDocs.documentList.forEach { doc ->
                documentValidation(doc)
                doc.checksum.checksum?.let { newDocChecksum ->
                    if (existingDocChecksums[newDocChecksum] != true) { // TODO: Confirm if duplicates silently ignored, or do like AppendLoanStates
                        newDocList.addDocument(doc)
                        existingDocChecksums[newDocChecksum] = true
                    }
                }
            }
        }
        return newDocList.build()
    }
}
