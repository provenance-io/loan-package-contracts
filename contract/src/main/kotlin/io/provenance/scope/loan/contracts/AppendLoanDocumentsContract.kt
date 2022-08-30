package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.DocumentRecordingGuidance
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.LoanScopeProperties.servicingDocumentsKey
import io.provenance.scope.loan.utility.ContractRequirementType.VALID_INPUT
import io.provenance.scope.loan.utility.documentModificationValidation
import io.provenance.scope.loan.utility.documentValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.raiseError
import io.provenance.scope.loan.utility.unpackOrNull
import io.provenance.scope.loan.utility.updateServicingData
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.util.v1beta1.DocumentMetadata

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanDocumentsContract(
    @Record(name = LoanScopeFacts.documents, optional = true) val existingDocs: LoanDocuments?,
    @Record(name = LoanScopeFacts.servicingData, optional = true) val existingServicingData: ServicingData?,
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
                    documentMetadata.checksum.checksum.takeIf { it.isNotBlank() }?.let { checksum ->
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

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.servicingData)
    open fun appendServicingDocuments(@Input(LoanScopeFacts.documents) newDocs: LoanDocuments): ServicingData {
        return newDocs.metadataKvMap[servicingDocumentsKey]?.unpackOrNull<DocumentRecordingGuidance>()?.let { servicingDocumentGuidance ->
            ServicingData.newBuilder().also { servicingDataBuilder ->
                servicingDataBuilder.addAllDocMeta(
                    newDocs.documentList.filter { document ->
                        document.id.isSet() && servicingDocumentGuidance.containsDesignatedDocuments(document.id.value)
                    }
                )
            }.build()
        }?.let { wrappedServicingDocuments ->
            validateRequirements(VALID_INPUT) {
                updateServicingData(
                    existingServicingData = existingServicingData ?: ServicingData.getDefaultInstance(),
                    newServicingData = wrappedServicingDocuments,
                    expectLoanStates = false,
                )
            }
        } ?: existingServicingData ?: ServicingData.getDefaultInstance()
    }
}
