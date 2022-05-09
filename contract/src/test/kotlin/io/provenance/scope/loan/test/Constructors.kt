package io.provenance.scope.loan.test

import io.provenance.scope.loan.contracts.RecordLoanValidationResultsContract
import io.provenance.scope.util.toProtoTimestamp
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationItem
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest
import tech.figure.validation.v1beta1.ValidationResponse
import tech.figure.validation.v1beta1.ValidationResults
import java.time.OffsetDateTime
import java.util.UUID as JavaUUID
import tech.figure.util.v1beta1.UUID as FigureTechUUID

object Constructors {
    val randomProtoUuid: FigureTechUUID
        get() = FigureTechUUID.newBuilder().apply {
            value = JavaUUID.randomUUID().toString()
        }.build()

    val contractWithEmptyExistingValidationRecord: RecordLoanValidationResultsContract
        get() = RecordLoanValidationResultsContract(
            LoanValidation.getDefaultInstance()
        )
    fun contractWithSingleValidationIteration(
        requestID: FigureTechUUID,
        validatorName: String = "anotherRandomProviderName",
    ) = RecordLoanValidationResultsContract(
        LoanValidation.newBuilder().also { validationRecordBuilder ->
            validationRecordBuilder.clearIteration()
            validationRecordBuilder.addIteration(
                ValidationIteration.newBuilder().also { iterationBuilder ->
                    iterationBuilder.request = ValidationRequest.newBuilder().also { requestBuilder ->
                        requestBuilder.requestId = requestID
                        requestBuilder.ruleSetId = randomProtoUuid
                        requestBuilder.effectiveTime = OffsetDateTime.now().toProtoTimestamp()
                        requestBuilder.validatorName = validatorName
                    }.build()
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
