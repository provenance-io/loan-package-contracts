package io.provenance.scope.loan.contracts

import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.utility.LoanPackageContract
import io.provenance.scope.loan.utility.isValid
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.loan.v1beta1.LoanDocuments

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
@LoanPackageContract("AppendLoanDocContract")
open class AppendLoanDocContract(
    @Record(LoanScopeFacts.documents) val existingDocs: LoanDocuments,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.documents)
    open fun appendDocuments(@Input(LoanScopeFacts.documents) newDocs: LoanDocuments): LoanDocuments {
        val newDocList = LoanDocuments.newBuilder().mergeFrom(existingDocs)
        for (doc in newDocs.documentList) {
            validateRequirements(
                doc.id.isValid()              orError "Document missing ID",
                doc.uri.isNotBlank()          orError "Document with ID ${doc.id} is missing uri",
                doc.contentType.isNotBlank()  orError "Document with ID ${doc.id} is missing content type",
                doc.documentType.isNotBlank() orError "Document with ID ${doc.id} is missing document type",
                doc.checksum.isValid()        orError "Document with ID ${doc.id} is missing checksum",
            )
            if (!existingDocs.documentList.any { it.checksum == doc.checksum }) { // TODO: Improve efficiency of this
                newDocList.addDocument(doc)
            }
        }
        return newDocList.build()
    }
}
