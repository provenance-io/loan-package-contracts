package io.provenance

import io.p8e.annotations.Fact

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
    @Fact(LoanScopeFacts.asset) var asset: tech.figure.asset.v1beta1.Asset,
    @Fact(LoanScopeFacts.servicingRights) var servicingRights: tech.figure.servicing.v1beta1.ServicingRights, // Defaults to the lender

    // Optional
    @Fact(LoanScopeFacts.documents) var documents: tech.figure.util.v1beta1.LoanDocuments? = null, // list of document metadata with URIs that point to documents in EOS
    @Fact(LoanScopeFacts.loanStates) var loanStates: tech.figure.servicing.v1beta1.LoanStateList? = null, // list of loan states
    @Fact(LoanScopeFacts.validationResults) var validationResults: tech.figure.validation.v1beta1.ValidationResults? = null, // object containing list of third party validation results and a summary of exceptions

    // Loans registered with DART would include:
    @Fact(LoanScopeFacts.eNote) var eNote: io.dartinc.registry.v1beta1.ENote? = null

)