package io.provenance.scope.loan.utility

import io.dartinc.registry.v1beta1.ENote
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.util.v1beta1.DocumentMetadata
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationRequest
import tech.figure.validation.v1beta1.ValidationResults
import io.dartinc.registry.v1beta1.Controller as ENoteController

internal fun ContractEnforcementContext.documentModificationValidation(
    existingDocument: DocumentMetadata,
    newDocument: DocumentMetadata,
) {
    existingDocument.checksum.checksum.let { existingChecksum ->
        // TODO: Should we raise a violation if the checksum's algorithm is changed?
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
    document.takeIf { it.isSet() }?.let { setDocument ->
        val documentIdSnippet = if (setDocument.id.isSet()) {
            " with ID ${setDocument.id.value}"
        } else {
            ""
        }
        requireThat(
            setDocument.id.isValid()              orError "Document must have valid ID",
            setDocument.uri.isNotBlank()          orError "Document$documentIdSnippet is missing URI",
            setDocument.contentType.isNotBlank()  orError "Document$documentIdSnippet is missing content type",
            setDocument.documentType.isNotBlank() orError "Document$documentIdSnippet is missing document type",
            setDocument.checksum.isValid()        orError "Document$documentIdSnippet is missing checksum",
        )
    } ?: raiseError("Document is not set")
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
            loanDocuments.documentList.isNotEmpty() orError "Must supply at least one document"
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
            val incomingIterationRequestIds = mutableMapOf<String, UInt>()
            validationRecord.iterationList.requireThatEach { iteration ->
                iteration.request.requestId.takeIf { it.isSet() }?.value?.let { iterationId ->
                    incomingIterationRequestIds[iterationId] = incomingIterationRequestIds.getOrDefault(iterationId, 0U) + 1U
                }
                /** TODO: Find out if there is another way to do this, instead of having to concatenate lists */
                return@requireThatEach loanValidationRequestValidation(iteration.request) + loanValidationResultsValidation(iteration.results)
            }
            incomingIterationRequestIds.entries.forEach { (iterationId, count) ->
                if (count > 1U) {
                    raiseError("Request ID $iterationId is not unique ($count usages)")
                }
            }
        } else {
            raiseError("Must supply at least one validation iteration")
        }
    }
}

internal val loanValidationResultsValidation: ContractEnforcementContext.(ValidationResults) -> List<ContractEnforcement> = { results ->
    results.takeIf { it.isSet() }?.let { setResults ->
        requireThat(
            setResults.resultSetUuid.isValid()          orError "Results must have valid result set UUID",
            setResults.resultSetEffectiveTime.isValid() orError "Results are missing timestamp",
            (setResults.validationExceptionCount >= 0)  orError "Results report an invalid validation exception count",
            (setResults.validationWarningCount >= 0)    orError "Results report an invalid validation warning count",
            (setResults.validationItemsCount > 0)       orError "Results must have at least one validation item",
            setResults.resultSetProvider.isNotBlank()   orError "Results missing provider name",
        )
    } ?: raiseError("Results are not set")
}

internal val loanValidationRequestValidation: ContractEnforcementContext.(ValidationRequest) -> List<ContractEnforcement> = { request ->
    request.takeIf { it.isSet() }?.let { setRequest ->
        requireThat(
            setRequest.requestId.isValid()        orError "Request must have valid ID",
            setRequest.effectiveTime.isValid()    orError "Request is missing timestamp",
            setRequest.snapshotUri.isNotBlank()   orError "Request is missing loan snapshot URI", // TODO: Change to block height in model v0.1.9
            setRequest.validatorName.isNotBlank() orError "Request is missing validator name",
            setRequest.requesterName.isNotBlank() orError "Request is missing requester name",
        )
    } ?: raiseError("Request is not set")
}

internal val servicingRightsInputValidation: (ServicingRights) -> Unit = { servicingRights ->
    validateRequirements(ContractRequirementType.VALID_INPUT,
        servicingRights.servicerId.isValid()      orError "Servicing rights must have valid servicer UUID",
        servicingRights.servicerName.isNotBlank() orError "Servicing rights missing servicer name",
    )
}

internal val uliValidation: ContractEnforcementContext.(String) -> Unit = { uli ->
    // TODO: Investigate wrapping certain protoc validate.rules call into a ContractEnforcement
    requireThat((uli.length in 23..45) orError "Loan ULI is invalid") // TODO: Any other requirements for ULI that our contracts can enforce?
}
