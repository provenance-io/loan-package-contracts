package io.provenance.scope.loan

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecificationDefinition
import io.provenance.scope.contract.proto.Specifications
import io.provenance.scope.contract.spec.P8eScopeSpecification
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.LoanValidation

object LoanScopeFacts {
    const val asset = "asset"
    const val documents = "documents"
    const val servicingRights = "servicingRights"
    const val servicingData = "servicingData"
    const val loanValidations = "loanValidations"
    const val eNote = "eNote"
}

data class LoanPackage(
    @Record(LoanScopeFacts.asset) var asset: Asset,
    @Record(LoanScopeFacts.servicingRights) var servicingRights: ServicingRights, // Defaults to the lender
    @Record(LoanScopeFacts.documents) var documents: LoanDocuments? = null, // list of document metadata with URIs that point to documents in EOS
    @Record(LoanScopeFacts.servicingData) var servicingData: ServicingData? = null, // list of loan state metadata
    @Record(LoanScopeFacts.loanValidations) var loanValidations: LoanValidation? = null, // object containing list of third party validation results and a summary of exceptions
    @Record(LoanScopeFacts.eNote) var eNote: ENote? = null
)

@ScopeSpecificationDefinition(
    uuid = "c370d852-0f3b-4f70-85e6-25983ac07c0f",
    name = "tech.figure.loan",
    description = "An extensible loan scope",
    partiesInvolved = [Specifications.PartyType.OWNER],
)
open class LoanScopeSpecification : P8eScopeSpecification()
