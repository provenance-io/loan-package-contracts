package io.provenance.scope.loan.contracts

import com.fasterxml.jackson.databind.ObjectMapper
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.WordSpec
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.next
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidAsset
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENote
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoan
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingData
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingRights
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationRecord
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.loan.v1beta1.Loan as FigureTechLoan

@Ignored
internal class RandomLoanGenerationExperiment : WordSpec({
    /* Helpers */
    val mapper = ObjectMapper().also { objectMapper ->
        objectMapper.registerModule(ProtobufModule())
        objectMapper.writerWithDefaultPrettyPrinter()
    }
    val randomSource = RandomSource.default()
    /* JSON generators */
    "KotestHelpers" should {
        "be able to generate a random fully populated Figure Tech loan scope" {
            anyValidLoan<FigureTechLoan>().next(randomSource).let { randomLoanPackage ->
                println(
                    mapper.writeValueAsString(
                        mapOf(
                            LoanScopeFacts.asset to randomLoanPackage.asset,
                            LoanScopeFacts.eNote to randomLoanPackage.eNote,
                            LoanScopeFacts.servicingRights to randomLoanPackage.servicingRights,
                            LoanScopeFacts.servicingData to randomLoanPackage.servicingData,
                            LoanScopeFacts.loanValidations to randomLoanPackage.loanValidations,
                            LoanScopeFacts.documents to randomLoanPackage.documents,
                        )
                    )
                )
            }
        }
        "be able to generate a random fully populated MISMO loan scope" {
            anyValidLoan<MISMOLoanMetadata>().next(randomSource).let { randomLoanPackage ->
                println(
                    mapper.writeValueAsString(
                        mapOf(
                            LoanScopeFacts.asset to randomLoanPackage.asset,
                            LoanScopeFacts.eNote to randomLoanPackage.eNote,
                            LoanScopeFacts.servicingRights to randomLoanPackage.servicingRights,
                            LoanScopeFacts.servicingData to randomLoanPackage.servicingData,
                            LoanScopeFacts.loanValidations to randomLoanPackage.loanValidations,
                            LoanScopeFacts.documents to randomLoanPackage.documents,
                        )
                    )
                )
            }
        }
        "!be able to generate a random asset for a Figure Tech loan" {
            // TODO: Convert to JsonNode and alter loan value JsonNode to unpacked version
            println(
                mapper.writeValueAsString(anyValidAsset<FigureTechLoan>().next(randomSource))
            )
        }
        "!be able to generate a random asset for a MISMO loan" {
            // TODO: Convert to JsonNode and alter loan value JsonNode to unpacked version
            println(
                mapper.writeValueAsString(anyValidAsset<MISMOLoanMetadata>().next(randomSource))
            )
        }
        "be able to generate a random eNote" {
            println(
                mapper.writeValueAsString(anyValidENote().next(randomSource))
            )
        }
        "be able to generate random servicing rights" {
            println(
                mapper.writeValueAsString(anyValidServicingRights.next(randomSource))
            )
        }
        "be able to generate random servicing data" {
            println(
                mapper.writeValueAsString(anyValidServicingData(loanStateCount = 4).next(randomSource))
            )
        }
        "be able to generate a random validation record" {
            println(
                mapper.writeValueAsString(anyValidValidationRecord(iterationCount = 4).next(randomSource))
            )
        }
        "be able to generate random loan documents" {
            println(
                mapper.writeValueAsString(anyValidLoanDocumentSet(size = 4).next(randomSource))
            )
        }
    }
})
