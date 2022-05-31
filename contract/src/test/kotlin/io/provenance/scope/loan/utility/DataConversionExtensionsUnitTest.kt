package io.provenance.scope.loan.utility

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.LoanPackageArbs
import io.provenance.scope.loan.test.shouldBeParseFailureFor
import tech.figure.proto.util.toProtoAny
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.UUID as FigureTechUUID

class DataConversionExtensionsUnitTest : WordSpec({
    "unpackAs" When {
        "given a packed protobuf to unpack as the same type it was packed as" should {
            "not throw an exception" {
                checkAll(LoanPackageArbs.anyValidChecksum) { randomChecksum ->
                    shouldNotThrow<UnexpectedContractStateException> {
                        randomChecksum.toProtoAny().unpackAs<FigureTechChecksum>() shouldBe randomChecksum
                    }
                }
            }
        }
        "given a packed protobuf to unpack as a type with inherently different fields than the type it was packed as" should {
            "throw an appropriate exception" {
                checkAll(LoanPackageArbs.anyValidChecksum) { randomChecksum ->
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
        "given a packed protobuf to unpack as the same type it was packed as" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
        "given a packed protobuf to unpack as a type with inherently different fields than the type it was packed as" xshould {
            "throw an appropriate exception" {
                // TODO: Implement
            }
        }
        "given a packed Figure Tech loan to unpack as a MISMO loan with inherently different fields" xshould {
            "throw an appropriate informative exception" {
                // TODO: Implement
            }
        }
        "given a packed MISMO loan to unpack as a Figure Tech loan with inherently different fields" xshould {
            "throw an appropriate informative exception" {
                // TODO: Implement
            }
        }
    }
    "toFigureTechLoan" When {
        "called on a non-null inapplicable protobuf" should {
            "throw an appropriate exception for unpacking" {
                checkAll(Arb.string(), Arb.string()) { randomChecksumString, randomAlgorithmString ->
                    FigureTechChecksum.newBuilder().apply {
                        checksum = randomChecksumString
                        algorithm = randomAlgorithmString
                    }.build().let { randomChecksum ->
                        shouldThrow<UnexpectedContractStateException> {
                            randomChecksum?.toProtoAny()?.toFigureTechLoan()
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
        }
        "called on a packed Figure Tech loan" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
    "toMISMOLoan" xshould {
        "called on a non-null inapplicable protobuf" xshould {
            "throw an appropriate exception for unpacking" {
                // TODO: Implement
            }
        }
        "called on a packed MISMO loan" xshould {
            "not throw an exception" {
                // TODO: Implement
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
