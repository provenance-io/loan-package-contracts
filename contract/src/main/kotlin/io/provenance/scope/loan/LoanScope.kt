package io.provenance.scope.loan

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecificationDefinition
import io.provenance.scope.contract.proto.Specifications
import io.provenance.scope.contract.spec.P8eScopeSpecification
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStates
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.ValidationResults

object LoanScopeFacts {

    const val asset = "asset"
    const val documents = "documents"
    const val loanStates = "loan_states"
    const val servicingRights = "servicing_rights"
    const val validationResults = "validation_results"

    // Loans registered with DART would include:
    const val eNote = "e_note"

}

data class LoanPackage(

    // Required
    @Record(LoanScopeFacts.asset) var asset: Asset,
    @Record(LoanScopeFacts.servicingRights) var servicingRights: ServicingRights, // Defaults to the lender

    // Optional
    @Record(LoanScopeFacts.documents) var documents: LoanDocuments? = null, // list of document metadata with URIs that point to documents in EOS
    @Record(LoanScopeFacts.loanStates) var loanStates: LoanStates? = null, // list of loan states
    @Record(LoanScopeFacts.validationResults) var validationResults: ValidationResults? = null, // object containing list of third party validation results and a summary of exceptions

    // Loans registered with DART would include:
    @Record(LoanScopeFacts.eNote) var eNote: ENote? = null

)

@ScopeSpecificationDefinition(
    uuid = "c370d852-0f3b-4f70-85e6-25983ac07c0f",
    name = "tech.figure.loan",
    description = "An extensible loan scope",
    partiesInvolved = [Specifications.PartyType.OWNER],
)
open class LoanScopeSpecification : P8eScopeSpecification()
