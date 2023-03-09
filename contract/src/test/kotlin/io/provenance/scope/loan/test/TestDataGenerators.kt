package io.provenance.scope.loan.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import io.dartinc.registry.v1beta1.DocumentRecordingGuidance
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.WordSpec
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.next
import io.provenance.scope.loan.LoanScopeFacts
import io.provenance.scope.loan.LoanScopeProperties.servicingDocumentsKey
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidAsset
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENote
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoan
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidLoanDocumentSet
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingData
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidServicingRights
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidValidationRecord
import tech.figure.proto.util.toProtoAny

@Ignored
internal class TestDataGenerators : WordSpec({
    /* Helpers */
    val mapper = ObjectMapper().also { objectMapper ->
        objectMapper.registerModule(ProtobufModule())
        objectMapper.writerWithDefaultPrettyPrinter()
    }
    val randomSource = RandomSource.default()
    /* JSON generators */
    "KotestHelpers" should {
        "be able to generate a random fully populated loan scope without a MISMO loan" {
            anyValidLoan(hasMismoLoan = false).next(randomSource).let { randomLoanPackage ->
                println(
                    mapper.writeValueAsString(
                        mapOf(
                            LoanScopeFacts.asset to randomLoanPackage.asset,
                            LoanScopeFacts.eNote to randomLoanPackage.eNote,
                            LoanScopeFacts.servicingRights to randomLoanPackage.servicingRights,
                            LoanScopeFacts.servicingData to randomLoanPackage.servicingData,
                            LoanScopeFacts.loanValidationMetadata to randomLoanPackage.loanValidations,
                            LoanScopeFacts.documents to randomLoanPackage.documents,
                        )
                    )
                )
            }
        }
        "be able to generate a random fully populated loan scope with a MISMO loan" {
            anyValidLoan(hasMismoLoan = true).next(randomSource).let { randomLoanPackage ->
                println(
                    mapper.writeValueAsString(
                        mapOf(
                            LoanScopeFacts.asset to randomLoanPackage.asset,
                            LoanScopeFacts.eNote to randomLoanPackage.eNote,
                            LoanScopeFacts.servicingRights to randomLoanPackage.servicingRights,
                            LoanScopeFacts.servicingData to randomLoanPackage.servicingData,
                            LoanScopeFacts.loanValidationMetadata to randomLoanPackage.loanValidations,
                            LoanScopeFacts.documents to randomLoanPackage.documents,
                        )
                    )
                )
            }
        }
        "be able to generate a random asset for a Figure Tech loan" {
            println(
                mapper.writeValueAsString(anyValidAsset(hasMismoLoan = false, hasFunding = true).next(randomSource))
            )
        }
        "be able to generate a random asset for a Figure Tech loan with a MISMO loan" {
            println(
                mapper.writeValueAsString(anyValidAsset(hasMismoLoan = true, hasFunding = true).next(randomSource))
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
                mapper.writeValueAsString(anyValidServicingData(loanStateAndDocumentCount = 4).next(randomSource))
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
        "be able to generate random servicing documents" {
            println(
                mapper.writeValueAsString(
                    anyValidLoanDocumentSet(size = 1).next(randomSource).toBuilder().also { documentsBuilder ->
                        documentsBuilder.putMetadataKv(
                            servicingDocumentsKey,
                            DocumentRecordingGuidance.newBuilder().also { guidanceBuilder ->
                                guidanceBuilder.putDesignatedDocuments(
                                    documentsBuilder.getDocument(0).id.value,
                                    true
                                )
                            }.build().toProtoAny()
                        )
                    }.build()
                )
            )
        }
    }
})
