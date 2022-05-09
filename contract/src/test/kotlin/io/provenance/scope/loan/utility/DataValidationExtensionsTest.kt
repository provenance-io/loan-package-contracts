package io.provenance.scope.loan.utility

import com.google.protobuf.Any
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import io.provenance.scope.util.toProtoTimestamp
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest
import java.time.ZoneOffset
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
import tech.figure.util.v1beta1.Money as FigureTechMoney
import tech.figure.util.v1beta1.UUID as FigureTechUUID

class DataValidationExtensionsTest : WordSpec({
    "Message.isSet" should {
        "return false for any default instance" {
            ValidationIteration.getDefaultInstance().isSet() shouldBe false
            ValidationIteration.newBuilder().build().isSet() shouldBe false
            Any.getDefaultInstance().isSet() shouldBe false
            Any.newBuilder().build().isSet() shouldBe false
        }
        ValidationRequest.newBuilder().apply {
            requestId = randomProtoUuid
        }.build().let { modifiedValidationRequest ->
            "return true for any altered object" {
                modifiedValidationRequest.isSet() shouldBe true
            }
            "return false for specific fields in an altered object" {
                modifiedValidationRequest.effectiveTime.isSet() shouldBe false
            }
        }
    }
    "Timestamp.isValid" should {
        "return true for any UTC date" {
            forAll(Arb.localDateTime()) { randomLocalDateTime ->
                randomLocalDateTime.atOffset(ZoneOffset.UTC).toProtoTimestamp().isValid()
            }
        }
    }
    "Date.isValid" should {
        "return false for a default instance" {
            FigureTechDate.getDefaultInstance().isValid() shouldBe false
        }
        "return true for any valid date" {
            forAll(Arb.localDate()) { randomLocalDate ->
                FigureTechDate.newBuilder().apply {
                    value = randomLocalDate.toString()
                }.build().isValid()
            }
        }
    }
    "Checksum.isValid" should {
        "return false for a default instance" {
            FigureTechChecksum.getDefaultInstance().isValid() shouldBe false
        }
        "return false for an invalid instance" {
            FigureTechChecksum.newBuilder().apply {
                checksum = ""
            }.build().isValid() shouldBe false
        }
        "return true for any non-empty string" {
            forAll(Arb.string()) { randomString ->
                FigureTechChecksum.newBuilder().apply {
                    checksum = randomString
                }.build().isValid() == randomString.isNotBlank()
            }
        }
    }
    "FigureTechUUID.isValid" should {
        "return false for a default instance" {
            FigureTechUUID.getDefaultInstance().isValid() shouldBe false
        }
        "return false for an invalid instance" {
            checkAll(Arb.string(minSize = 1, maxSize = 35)) { randomShortString ->
                FigureTechUUID.newBuilder().apply {
                    value = randomShortString
                }.build().isValid() shouldBe false
            }
            checkAll(Arb.string(minSize = 37)) { randomLongString ->
                FigureTechUUID.newBuilder().apply {
                    value = randomLongString
                }.build().isValid() shouldBe false
            }
        }
        "return true for any valid instance" {
            forAll(Arb.uuid()) { randomJavaUuidV4 ->
                FigureTechUUID.newBuilder().apply {
                    value = randomJavaUuidV4.toString()
                }.build().isValid()
            }
        }
    }
    "Money.isValid" should {
        "return true for any double" {
            forAll(Arb.double()) { randomDouble ->
                FigureTechMoney.newBuilder().apply {
                    amount = randomDouble
                }.build().isValid()
            }
        }
    }
})
