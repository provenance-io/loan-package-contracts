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
    "tryUnpackingAs" should {
        "successfully unpack a packed protobuf as the same type that was packed" {
            checkAll(LoanPackageArbs.anyValidChecksum) { randomChecksum ->
                shouldNotThrow<UnexpectedContractStateException> {
                    randomChecksum.toProtoAny().tryUnpackingAs<FigureTechChecksum>() shouldBe randomChecksum
                }
            }
        }
        "fail to unpack a packed protobuf as a type with inherently different fields" {
            checkAll(LoanPackageArbs.anyValidChecksum) { randomChecksum ->
                shouldThrow<UnexpectedContractStateException> {
                    randomChecksum.toProtoAny().tryUnpackingAs<FigureTechUUID>()
                }.let { exception ->
                    exception shouldBeParseFailureFor "tech.figure.util.v1beta1.UUID"
                }
            }
        }
    }
    "toLoan" should {
        "throw an exception for unpacking when called on a non-nullable inapplicable protobuf" {
            checkAll(Arb.string(), Arb.string()) { randomChecksumString, randomAlgorithmString ->
                FigureTechChecksum.newBuilder().apply {
                    checksum = randomChecksumString
                    algorithm = randomAlgorithmString
                }.build().let { randomChecksum ->
                    shouldThrow<UnexpectedContractStateException> {
                        randomChecksum?.toProtoAny()?.toLoan()
                    }.let { exception ->
                        exception shouldBeParseFailureFor "tech.figure.loan.v1beta1.Loan"
                    }
                    IllegalArgumentException("Expected the receiver's algorithm to not be set").let { callerException ->
                        shouldThrow<IllegalArgumentException> { // Sanity check that parsing is only attempted when intended by code
                            randomChecksum.takeIf { false }?.toProtoAny()?.toLoan()
                                ?: throw callerException
                        }.let { thrownException ->
                            thrownException shouldBe callerException
                        }
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
