package io.provenance.scope.loan.utility

import io.dartinc.registry.v1beta1.ENote
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.util.v1beta1.DocumentMetadata
import tech.figure.validation.v1beta1.LoanValidation
import io.dartinc.registry.v1beta1.Controller as ENoteController

internal val documentModificationValidation: ContractEnforcementContext.(
    DocumentMetadata,
    DocumentMetadata,
) -> Unit = { existingDocument, newDocument ->
    existingDocument.checksum.checksum.let { existingChecksum ->
        if (existingChecksum == newDocument.checksum.checksum) {
            val checksumSnippet = if (existingChecksum.isNotBlank()) {
                " with checksum $existingChecksum"
            } else {
                ""
            }
            requireThat(
                (existingDocument.id == newDocument.id)
                    orError "Cannot change ID of existing document$checksumSnippet",
                (existingDocument.uri == newDocument.uri)
                    orError "Cannot change URI of existing document$checksumSnippet",
                (existingDocument.contentType == newDocument.contentType)
                    orError "Cannot change content type of existing document$checksumSnippet",
                (existingDocument.documentType == newDocument.documentType)
                    orError "Cannot change content type of existing document$checksumSnippet",
            )
        }
    }
}

internal val documentValidation: ContractEnforcementContext.(DocumentMetadata) -> Unit = { document ->
    val documentIdSnippet = if (document.id.isSet()) {
        " with ID ${document.id.value}"
    } else {
        ""
    }
    requireThat(
        document.id.isValid()              orError "Document must have valid ID",
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
        document.id.isValid()              orError "ENote must have valid ID",
        document.uri.isNotBlank()          orError "ENote is missing URI",
        document.contentType.isNotBlank()  orError "ENote is missing content type",
        document.documentType.isNotBlank() orError "ENote is missing document type",
        document.checksum.isValid()        orError "ENote is missing checksum",
    )
}

internal val eNoteInputValidation: (ENote) -> Unit = { eNote ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        eNoteValidation(eNote)
    }
}

internal val eNoteValidation: ContractEnforcementContext.(ENote) -> Unit = { eNote ->
    // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
    eNoteControllerValidation(eNote.controller)
    eNoteDocumentValidation(eNote.eNote)
    requireThat(
        eNote.signedDate.isValid()   orError "ENote is missing signed date",
        eNote.vaultName.isNotBlank() orError "ENote is missing vault name",
    )
}

internal val loanDocumentInputValidation: (LoanDocuments) -> Unit = { loanDocuments ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        requireThat(
            loanDocuments.documentList.isNotEmpty() orError "Must supply at least one document" // TODO: Verify desired; not thrown for optional input
        )
        val incomingDocChecksums = mutableMapOf<String, Boolean>()
        loanDocuments.documentList.forEach { document ->
            documentValidation(document)
            document.checksum.takeIf { it.isValid() }?.checksum?.let { checksum ->
                if (incomingDocChecksums[checksum] == true) {
                    raiseError("Loan document with checksum $checksum already provided in input")
                }
                incomingDocChecksums[checksum] = true
            }
        }
    }
}

internal val loanStateValidation: ContractEnforcementContext.(LoanStateMetadata) -> Unit = { loanState ->
    val idSnippet = if (loanState.id.isSet()) {
        " with ID ${loanState.id.value}"
    } else {
        ""
    }
    requireThat(
        loanState.id.isValid()                        orError "Loan state must have valid ID",
        loanState.effectiveTime.isValidForLoanState() orError "Loan state$idSnippet must have valid effective time",
        loanState.uri.isNotBlank()                    orError "Loan state$idSnippet is missing URI",
        loanState.checksum.isValid()                  orError "Loan state$idSnippet is missing checksum"
    )
}

internal val loanValidationInputValidation: (LoanValidation) -> Unit = { validationRecord ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        if (validationRecord.iterationCount > 0) {
            val incomingValidationRequests = mutableMapOf<String, Boolean>()
            validationRecord.iterationList.forEach { iteration ->
                // TODO: Implement - after DRYing logic in existing request and result recording contracts, in same manner as eNote
            }
        } else {
            raiseError("Must supply at least one validation iteration")
        }
    }
}

internal val servicingRightsInputValidation: (ServicingRights) -> Unit = { servicingRights ->
    validateRequirements(ContractRequirementType.VALID_INPUT,
        servicingRights.servicerId.isValid()      orError "Servicing rights must have valid servicer UUID",
        servicingRights.servicerName.isNotBlank() orError "Servicing rights missing servicer name",
    )
}
