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
        if (existingChecksum == newDocument.checksum.checksum) {
            val checksumSnippet = if (existingChecksum.isNotBlank()) {
                " with checksum $existingChecksum"
            } else {
                ""
            }
            requireThat(
                (existingDocument.checksum.algorithm == newDocument.checksum.algorithm)
                    orError "Cannot change checksum algorithm of existing document$checksumSnippet",
                (existingDocument.id == newDocument.id)
                    orError "Cannot change ID of existing document$checksumSnippet",
                (existingDocument.uri == newDocument.uri)
                    orError "Cannot change URI of existing document$checksumSnippet",
                (existingDocument.contentType == newDocument.contentType)
                    orError "Cannot change content type of existing document$checksumSnippet",
                (existingDocument.documentType == newDocument.documentType)
                    orError "Cannot change document type of existing document$checksumSnippet",
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
    controller.takeIf { it.isSet() }?.let { setController ->
        requireThat(
            setController.controllerUuid.isValid()    orError "Controller must have valid UUID",
            setController.controllerName.isNotBlank() orError "Controller is missing name",
        )
    } ?: raiseError("Controller is not set")
}

internal val eNoteDocumentValidation: ContractEnforcementContext.(DocumentMetadata) -> Unit = { document ->
    document.takeIf { it.isSet() }?.let { setENote ->
        requireThat(
            setENote.id.isValid()              orError "eNote must have valid ID",
            setENote.uri.isNotBlank()          orError "eNote is missing URI",
            setENote.contentType.isNotBlank()  orError "eNote is missing content type",
            setENote.documentType.isNotBlank() orError "eNote is missing document type",
            setENote.checksum.isValid()        orError "eNote is missing checksum",
        )
    } ?: raiseError("eNote document is not set")
}

internal val eNoteInputValidation: (ENote) -> Unit = { eNote ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        eNoteValidation(eNote)
    }
}

internal val eNoteValidation: ContractEnforcementContext.(ENote) -> Unit = { eNote ->
    // TODO: Decide which fields should only be required if DART is listed as mortgagee of record/active custodian
    eNote.takeIf { it.isSet() }?.let { setENote ->
        eNoteControllerValidation(setENote.controller)
        eNoteDocumentValidation(setENote.eNote)
        requireThat(
            setENote.signedDate.isValidForSignedDate() orError "eNote must have valid signed date",
            setENote.vaultName.isNotBlank()            orError "eNote is missing vault name",
        )
    } ?: raiseError("eNote is not set")
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
                    raiseError("Loan document with checksum $checksum is provided more than once in input")
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
            setResults.resultSetUuid.isValid()                        orError "Results must have valid result set UUID",
            setResults.resultSetEffectiveTime.isValidAndNotInFuture() orError "Results must have valid effective time",
            (setResults.validationExceptionCount >= 0)                orError "Results report an invalid validation exception count",
            (setResults.validationWarningCount >= 0)                  orError "Results report an invalid validation warning count",
            (setResults.validationItemsCount > 0)                     orError "Results must have at least one validation item",
            setResults.resultSetProvider.isNotBlank()                 orError "Results missing provider name",
        )
    } ?: raiseError("Results are not set")
}

internal val loanValidationRequestValidation: ContractEnforcementContext.(ValidationRequest) -> List<ContractEnforcement> = { request ->
    request.takeIf { it.isSet() }?.let { setRequest ->
        requireThat(
            setRequest.requestId.isValid()                   orError "Request must have valid ID",
            setRequest.effectiveTime.isValidAndNotInFuture() orError "Request must have valid effective time",
            (setRequest.blockHeight >= 0L)                   orError "Request must have valid block height",
            setRequest.validatorName.isNotBlank()            orError "Request is missing validator name",
            setRequest.requesterName.isNotBlank()            orError "Request is missing requester name",
        )
    } ?: raiseError("Request is not set")
}

internal val servicingRightsInputValidation: (ServicingRights) -> Unit = { servicingRights ->
    validateRequirements(ContractRequirementType.VALID_INPUT) {
        servicingRights.takeIf { it.isSet() }?.let { setServicingRights ->
            requireThat(
                setServicingRights.servicerId.isValid()      orError "Servicing rights must have valid servicer UUID",
                setServicingRights.servicerName.isNotBlank() orError "Servicing rights missing servicer name",
            )
        } ?: raiseError("Servicing rights are not set")
    }
}

internal val uliValidation: ContractEnforcementContext.(String) -> Unit = { uli ->
    // TODO: Investigate wrapping certain protoc validate.rules call into a ContractEnforcement
    requireThat(
        (uli.length in 23..45)                               orError "Loan ULI must be between 23 and 45 (inclusive) characters long",
        uli.all { character -> character.isLetterOrDigit() } orError "Loan ULI must solely consist of alphanumeric characters",
    )
}
