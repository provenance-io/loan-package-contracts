package io.provenance.scope.loan.test

import io.provenance.scope.loan.contracts.RecordLoanValidationResultsContract
import io.provenance.scope.util.toProtoTimestamp
import tech.figure.validation.v1beta1.LoanValidation
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest
import tech.figure.validation.v1beta1.ValidationResponse
import java.time.OffsetDateTime
import tech.figure.util.v1beta1.UUID as FigureTechUUID

object Constructors {
    val randomProtoUuid: FigureTechUUID
        get() = FigureTechUUID.newBuilder().apply {
            value = java.util.UUID.randomUUID().toString()
        }.build()

    val contractWithEmptyExistingValidationRecord: RecordLoanValidationResultsContract
        get() = RecordLoanValidationResultsContract(
            LoanValidation.getDefaultInstance()
        )
    fun contractWithSingleValidationIteration(requestID: FigureTechUUID) = RecordLoanValidationResultsContract(
        LoanValidation.newBuilder().also { validationRecordBuilder ->
            validationRecordBuilder.clearIteration()
            validationRecordBuilder.addIteration(
                ValidationIteration.newBuilder().also { iterationBuilder ->
                    iterationBuilder.request = ValidationRequest.newBuilder().also { requestBuilder ->
                        requestBuilder.requestId = requestID
                        requestBuilder.ruleSetId = randomProtoUuid
                        requestBuilder.effectiveTime = OffsetDateTime.now().toProtoTimestamp()
                    }.build()
                }.build()
            )
        }.build()
    )
    fun validResultSubmission(iterationRequestID: FigureTechUUID) = ValidationResponse.newBuilder().apply {
        requestId = iterationRequestID
        // TODO: Set remaining fields
    }.build()
}
