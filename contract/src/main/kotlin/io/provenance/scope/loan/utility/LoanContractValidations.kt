package io.provenance.scope.loan.utility

import io.dartinc.registry.v1beta1.ENote
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.util.v1beta1.DocumentMetadata
import io.dartinc.registry.v1beta1.Controller as ENoteController

internal val documentListInputValidation: (LoanDocuments) -> Unit = { documents ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        documents.documentList.forEach { document ->
            documentValidation(document)
        }
    }
}

internal val documentValidation: ContractEnforcementContext.(DocumentMetadata) -> Unit = { document ->
    val documentIdSnippet = if (document.id.isSet()) {
        " with ID ${document.id}"
    } else {
        ""
    }
    requireThat(
        document.id.isValid()              orError "Document missing ID",
        document.uri.isNotBlank()          orError "Document$documentIdSnippet is missing URI",
        document.contentType.isNotBlank()  orError "Document$documentIdSnippet is missing content type",
        document.documentType.isNotBlank() orError "Document$documentIdSnippet is missing document type",
        document.checksum.isValid()        orError "Document$documentIdSnippet is missing checksum",
    )
}

internal val eNoteControllerValidation: ContractEnforcementContext.(ENoteController) -> Unit = { controller ->
    requireThat(
        controller.controllerUuid.isValid()    orError "Missing controller UUID",
        controller.controllerName.isNotBlank() orError "Missing controller name",
    )
}

internal val eNoteDocumentValidation: ContractEnforcementContext.(DocumentMetadata) -> Unit = { document ->
    requireThat(
        document.id.isValid()              orError "ENote missing ID",
        document.uri.isNotBlank()          orError "ENote missing URI",
        document.contentType.isNotBlank()  orError "ENote missing content type",
        document.documentType.isNotBlank() orError "ENote missing document type",
        document.checksum.isValid()        orError "ENote missing checksum",
    )
}

internal val eNoteValidation: ContractEnforcementContext.(ENote) -> Unit = { eNote ->
    // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
    eNoteControllerValidation(eNote.controller)
    eNoteDocumentValidation(eNote.eNote)
    requireThat(
        eNote.signedDate.isValid()   orError "ENote missing signed date",
        eNote.vaultName.isNotBlank() orError "ENote missing vault name",
    )
}

internal val servicingRightsInputValidation: (ServicingRights) -> Unit = { servicingRights ->
    validateRequirements(ContractRequirementType.VALID_INPUT,
        servicingRights.servicerId.isValid()      orError "Missing servicer UUID",
        servicingRights.servicerName.isNotBlank() orError "Missing servicer name",
    )
}
