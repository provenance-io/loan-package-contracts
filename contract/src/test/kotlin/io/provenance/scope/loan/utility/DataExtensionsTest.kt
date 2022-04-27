package io.provenance.scope.loan.utility

import com.google.protobuf.Any
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.provenance.scope.loan.test.Constructors.randomProtoUuid
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest

class DataExtensionsTest : WordSpec({
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
})
