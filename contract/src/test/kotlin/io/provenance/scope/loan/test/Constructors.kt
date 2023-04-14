package io.provenance.scope.loan.test

import io.dartinc.registry.v1beta1.ENote
import io.provenance.scope.loan.contracts.AppendLoanStatesContract
import io.provenance.scope.loan.contracts.RecordLoanContract
import io.provenance.scope.loan.contracts.RecordLoanValidationRequestContract
import io.provenance.scope.loan.contracts.RecordLoanValidationResultsContract
import io.provenance.scope.loan.contracts.RecordStandaloneLoanValidationResultsContract
import tech.figure.asset.v1beta1.Asset
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.validation.v1beta2.LoanValidation
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
    val standaloneResultsContractWithEmptyExistingRecord: RecordStandaloneLoanValidationResultsContract
        get() = RecordStandaloneLoanValidationResultsContract(
            LoanValidation.getDefaultInstance()
        )
    val requestContractWithEmptyExistingRecord: RecordLoanValidationRequestContract
        get() = RecordLoanValidationRequestContract(
            LoanValidation.getDefaultInstance()
        )
}
