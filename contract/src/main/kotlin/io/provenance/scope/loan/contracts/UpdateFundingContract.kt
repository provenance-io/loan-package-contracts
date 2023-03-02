package io.provenance.scope.loan.contracts

import io.provenance.scope.contract.annotations.Function
import io.provenance.scope.contract.annotations.Input
import io.provenance.scope.contract.annotations.Participants
import io.provenance.scope.contract.annotations.Record
import io.provenance.scope.contract.annotations.ScopeSpecification
import io.provenance.scope.contract.proto.Specifications.PartyType
import io.provenance.scope.contract.spec.P8eContract
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.LoanScopeInputs
import io.provenance.scope.loan.LoanScopeProperties.assetLoanKey
import io.provenance.scope.loan.utility.ContractRequirementType
import io.provenance.scope.loan.utility.falseIfError
import io.provenance.scope.loan.utility.fundingValidation
import io.provenance.scope.loan.utility.isSet
import io.provenance.scope.loan.utility.orError
import io.provenance.scope.loan.utility.toFigureTechLoan
import io.provenance.scope.loan.utility.validateRequirements
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.Funding
import tech.figure.proto.util.toProtoAny

@Participants(roles = [PartyType.OWNER])
@ScopeSpecification(["tech.figure.loan"])
open class UpdateFundingContract(
    @Record(name = LoanScopeFacts.asset, optional = false) val existingAsset: Asset,
) : P8eContract() {

    @Function(invokedBy = PartyType.OWNER)
    @Record(LoanScopeFacts.asset)
    open fun updateFunding(@Input(LoanScopeInputs.funding) newFunding: Funding): Asset {
        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
            requireThat(
                (falseIfError {
                    existingAsset.kvMap[assetLoanKey]!!.toFigureTechLoan().isSet()
                }) orError "An existing loan must be defined in the asset"
            )
        }
        validateRequirements(ContractRequirementType.VALID_INPUT) {
            fundingValidation(newFunding)
        }
        val newLoan = existingAsset.kvMap[assetLoanKey]!!.toFigureTechLoan().toBuilder().also { existingLoanBuilder ->
            existingLoanBuilder.funding = newFunding
        }.build()
        return existingAsset.toBuilder().also { existingAssetBuilder ->
            existingAssetBuilder.putKv(assetLoanKey, newLoan.toProtoAny())
        }.build()
    }
}
