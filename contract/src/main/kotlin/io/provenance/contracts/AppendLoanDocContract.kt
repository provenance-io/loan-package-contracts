package io.provenance.contracts

import io.p8e.annotations.Input
import io.p8e.annotations.Participants
import io.p8e.annotations.ScopeSpecification
import io.provenance.LoanScopeFacts
import io.p8e.proto.ContractSpecs.PartyType.OWNER
import io.p8e.spec.P8eContract

@Participants([OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class AppendLoanDocContract(
    @Record(LoanScopeFacts.documents) val existingDocs: tech.figure.util.v1beta1.LoanDocuments,
) : P8eContract() {

    @Function(OWNER)
    @Record(LoanScopeFacts.documents)
    open fun appendDocuments(@Input(LoanScopeFacts.documents) newDocs: tech.figure.util.v1beta1.LoanDocuments) {
        var newDocList = LoanDocuments.newBuilder().mergeFrom(existingDocs)
        for (doc in newDocs.documentList) {
            require(doc.id.isValid()) { "Document missing ID" }
            require(doc.uri.isNotBlank()) { "Document with ID ${doc.id} is missing uri" }
            require(doc.content_type.isNotBlank()) { "Document with ID ${doc.id} is missing content type" }
            require(doc.document_type.isNotBlank()) { "Document with ID ${doc.id} is missing document type" }
            require(doc.checksum.isNotBlank()) { "Document with ID ${doc.id} is missing checksum" }
            if (!existingDocs.documentList.any({ it.checksum == doc.checksum })) {
                newDocList.addDocument(doc)
            }
        }
        return newDocList.build()
    }
}