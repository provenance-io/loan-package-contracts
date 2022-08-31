package io.provenance.scope.loan.utility

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidChecksum
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidFigureTechLoan
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidMismoLoan
import io.provenance.scope.loan.test.shouldBeParseFailureFor
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.proto.util.toProtoAny
import tech.figure.loan.v1beta1.Loan as FigureTechLoan
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.UUID as FigureTechUUID

class DataConversionExtensionsUnitTest : WordSpec({
    "unpackAs" When {
        "given a packed protobuf to unpack as the same type it was packed as" should {
            "not throw an exception" {
                checkAll(anyValidChecksum) { randomChecksum ->
                    randomChecksum.toProtoAny().unpackAs<FigureTechChecksum>() shouldBe randomChecksum
                }
            }
        }
        "given a packed protobuf to unpack as a type with inherently different fields than the type it was packed as" should {
            "throw an appropriate exception" {
                checkAll(anyValidChecksum) { randomChecksum ->
                    shouldThrow<UnexpectedContractStateException> {
                        randomChecksum.toProtoAny().unpackAs<FigureTechUUID>()
                    }.let { exception ->
                        exception shouldBeParseFailureFor "tech.figure.util.v1beta1.UUID"
                    }
                }
            }
        }
    }
    "tryUnpackingAs" When {
        "given a packed protobuf to unpack as the same type it was packed as" should {
            "not throw an exception" {
                checkAll(anyUuid) { randomId ->
                    validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                        randomId.toProtoAny().tryUnpackingAs<FigureTechUUID, Unit> { unpacked ->
                            unpacked shouldBe randomId
                        }
                    }
                }
            }
            "return the result of its body" {
                checkAll(anyUuid) { randomId ->
                    validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                        randomId.toProtoAny().tryUnpackingAs<FigureTechUUID, Boolean> { unpacked ->
                            unpacked.isValid()
                        } shouldBe true
                    }
                }
            }
        }
        "given a packed protobuf to unpack as a type with inherently different fields than the type it was packed as" should {
            "throw an appropriate exception" {
                checkAll(anyValidChecksum) { randomChecksum ->
                    shouldThrow<IllegalContractStateException> {
                        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                            randomChecksum.toProtoAny().tryUnpackingAs<FigureTechUUID, Unit> {}
                        }
                    }.let { exception ->
                        exception.shouldBeParseFailureFor(classifier = "tech.figure.util.v1beta1.UUID")
                    }
                }
            }
        }
        "given a packed Figure Tech loan to unpack as a MISMO loan with inherently different fields" should {
            "throw an appropriate informative exception" {
                checkAll(anyValidFigureTechLoan) { randomLoan ->
                    shouldThrow<IllegalContractStateException> {
                        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                            randomLoan.toProtoAny().tryUnpackingAs<MISMOLoanMetadata, Unit> {}
                        }
                    }.let { exception ->
                        exception.message shouldContain
                            "Expected input to be a ${MISMOLoanMetadata::class.java} but was actually a ${FigureTechLoan::class.java}"
                    }
                }
            }
        }
        "given a packed MISMO loan to unpack as a Figure Tech loan with inherently different fields" should {
            "throw an appropriate informative exception" {
                checkAll(anyValidMismoLoan) { randomLoan ->
                    shouldThrow<IllegalContractStateException> {
                        validateRequirements(ContractRequirementType.LEGAL_SCOPE_STATE) {
                            randomLoan.toProtoAny().tryUnpackingAs<FigureTechLoan, Unit> {}
                        }
                    }.let { exception ->
                        exception.message shouldContain
                            "Expected input to be a ${FigureTechLoan::class.java} but was actually a ${MISMOLoanMetadata::class.java}"
                    }
                }
            }
        }
    }
    "toFigureTechLoan" When {
        "called on a non-null inapplicable protobuf" should {
            "throw an appropriate exception for unpacking" {
                checkAll(anyValidChecksum) { randomChecksum ->
                    shouldThrow<UnexpectedContractStateException> {
                        randomChecksum.toProtoAny().toFigureTechLoan()
                    }.let { exception ->
                        exception shouldBeParseFailureFor "tech.figure.loan.v1beta1.Loan"
                    }
                    // Sanity check that parsing is only attempted when intended by code
                    IllegalArgumentException("Expected the receiver's algorithm to not be set").let { callerException ->
                        shouldThrow<IllegalArgumentException> {
                            randomChecksum.takeIf { false }?.toProtoAny()?.toFigureTechLoan()
                                ?: throw callerException
                        }.let { thrownException ->
                            thrownException shouldBe callerException
                        }
                    }
                }
            }
        }
        "called on a packed Figure Tech loan" should {
            "not throw an exception" {
                checkAll(anyValidFigureTechLoan) { randomLoan ->
                    randomLoan.toProtoAny().toFigureTechLoan() shouldBe randomLoan
                }
            }
        }
    }
    "toMISMOLoan" When {
        "called on a non-null inapplicable protobuf" should {
            "throw an appropriate exception for unpacking" {
                checkAll(anyValidChecksum) { randomChecksum ->
                    shouldThrow<UnexpectedContractStateException> {
                        randomChecksum.toProtoAny().toMISMOLoan()
                    }.let { exception ->
                        exception shouldBeParseFailureFor "tech.figure.loan.v1beta1.MISMOLoanMetadata"
                    }
                    // Sanity check that parsing is only attempted when intended by code
                    IllegalArgumentException("Expected the receiver's algorithm to not be set").let { callerException ->
                        shouldThrow<IllegalArgumentException> {
                            randomChecksum.takeIf { false }?.toProtoAny()?.toMISMOLoan()
                                ?: throw callerException
                        }.let { thrownException ->
                            thrownException shouldBe callerException
                        }
                    }
                }
            }
        }
        "called on a packed MISMO loan" should {
            "not throw an exception" {
                checkAll(anyValidMismoLoan) { randomLoan ->
                    randomLoan.toProtoAny().toMISMOLoan() shouldBe randomLoan
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
