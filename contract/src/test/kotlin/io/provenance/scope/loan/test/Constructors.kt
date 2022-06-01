package io.provenance.scope.loan.test

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.loan.contracts.AppendLoanStatesContract
import io.provenance.scope.loan.contracts.RecordLoanContract
import io.provenance.scope.loan.contracts.RecordLoanValidationRequestContract
import io.provenance.scope.loan.contracts.RecordLoanValidationResultsContract
import io.provenance.scope.util.toProtoTimestamp
import tech.figure.asset.v1beta1.Asset
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationItem
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest
import tech.figure.validation.v1beta1.ValidationResponse
import tech.figure.validation.v1beta1.ValidationResults
import java.time.OffsetDateTime
import kotlin.random.Random
import java.util.UUID as JavaUUID
import tech.figure.util.v1beta1.UUID as FigureTechUUID

object Constructors {
    val randomProtoUuid: FigureTechUUID
        get() = FigureTechUUID.newBuilder().apply {
            value = JavaUUID.randomUUID().toString()
        }.build()
    val appendLoanStatesContractWithNoExistingStates: AppendLoanStatesContract
        get() = AppendLoanStatesContract(
            existingServicingData = ServicingData.newBuilder().apply {
                clearLoanState()
            }.build()
        )
    val recordContractWithEmptyScope: RecordLoanContract
        get() = RecordLoanContract(
            existingAsset = Asset.getDefaultInstance(),
            existingENote = ENote.getDefaultInstance(),
            existingServicingData = ServicingData.getDefaultInstance(),
            existingServicingRights = ServicingRights.getDefaultInstance(),
        )
    val resultsContractWithEmptyExistingRecord: RecordLoanValidationResultsContract
        get() = RecordLoanValidationResultsContract(
            LoanValidation.getDefaultInstance()
        )

    val requestContractWithEmptyExistingRecord: RecordLoanValidationRequestContract
        get() = RecordLoanValidationRequestContract(
            LoanValidation.getDefaultInstance()
        )
    context(Random)
    fun validRequest(
        requestID: FigureTechUUID,
        requesterName: String = "someArbitraryRequesterName",
        validatorName: String = "yetAnotherRandomProviderName",
    ): ValidationRequest = ValidationRequest.newBuilder().also { requestBuilder ->
        requestBuilder.requestId = requestID
        requestBuilder.ruleSetId = randomProtoUuid
        requestBuilder.blockHeight = nextLong(0, Long.MAX_VALUE)
        requestBuilder.effectiveTime = OffsetDateTime.now().toProtoTimestamp()
        requestBuilder.requesterName = requesterName
        requestBuilder.validatorName = validatorName
    }.build()
    context(Random)
    fun resultsContractWithSingleRequest(
        requestID: FigureTechUUID,
        validatorName: String = "anotherRandomProviderName",
    ) = RecordLoanValidationResultsContract(
        LoanValidation.newBuilder().also { validationRecordBuilder ->
            validationRecordBuilder.clearIteration()
            validationRecordBuilder.addIteration(
                ValidationIteration.newBuilder().also { iterationBuilder ->
                    iterationBuilder.request = validRequest(
                        requestID = requestID,
                        validatorName = validatorName,
                    )
                }.build()
            )
        }.build()
    )
    fun validResultSubmission(
        iterationRequestID: FigureTechUUID = randomProtoUuid,
        resultSetID: FigureTechUUID = randomProtoUuid,
        resultSetProvider: String = "arbitraryProviderName",
    ): ValidationResponse = ValidationResponse.newBuilder().also { responseBuilder ->
        responseBuilder.requestId = iterationRequestID
        responseBuilder.results = ValidationResults.newBuilder().also { resultsBuilder ->
            resultsBuilder.resultSetUuid = resultSetID
            resultsBuilder.resultSetProvider = resultSetProvider
            resultsBuilder.resultSetEffectiveTime = OffsetDateTime.now().toProtoTimestamp()
            resultsBuilder.addValidationItems(
                ValidationItem.newBuilder().also { validationItemBuilder ->
                    validationItemBuilder.description = "Yep stuff passed I guess"
                }.build()
            )
        }.build()
    }.build()
}
