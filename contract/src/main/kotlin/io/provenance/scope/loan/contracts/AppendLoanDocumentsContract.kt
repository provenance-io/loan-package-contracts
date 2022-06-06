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
import io.provenance.scope.loan.utility.documentModificationValidation
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.raiseError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.util.v1beta1.DocumentMetadata

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanDocumentsContract(
    @Record(name = LoanScopeFacts.documents, optional = true) val existingDocs: LoanDocuments?,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun appendDocuments(@Input(LoanScopeFacts.documents) newDocs: LoanDocuments): LoanDocuments {
        val newDocList = existingDocs?.toBuilder() ?: LoanDocuments.newBuilder()
        validateRequirements(VALID_INPUT) {
            /* Primitive type used for protobuf keys to avoid comparison interference from unknown fields */
            val existingDocumentMetadata = mutableMapOf<String, DocumentMetadata>()
            if (newDocs.documentList.isNotEmpty()) {
                existingDocs?.documentList?.forEach { documentMetadata ->
                    documentMetadata.checksum.takeIf { it.isValid() }?.checksum?.let { checksum ->
                        existingDocumentMetadata[checksum] = documentMetadata
                    }
                }
            } else {
                raiseError("Must supply at least one document")
            }
            val incomingDocChecksums = mutableMapOf<String, Boolean>()
            newDocs.documentList.forEach { newDocument ->
                documentValidation(newDocument)
                newDocument.checksum.checksum?.let { newDocChecksum ->
                    if (incomingDocChecksums[newDocChecksum] == true) {
                        raiseError("Loan document with checksum $newDocChecksum is provided more than once in input")
                    }
                    existingDocumentMetadata[newDocChecksum]?.let { existingDocument ->
                        documentModificationValidation(
                            existingDocument,
                            newDocument,
                        )
                    }
                    /* Append new data - if a violation was found, the eventual thrown exception prevents the changes from persisting */
                    newDocList.addDocument(newDocument)
                    incomingDocChecksums[newDocChecksum] = true
                }
            }
        }
        return newDocList.build()
    }
}
