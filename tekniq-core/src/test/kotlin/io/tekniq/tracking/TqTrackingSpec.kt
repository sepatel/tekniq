package io.tekniq.tracking

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

object TqTrackingSpec : DescribeSpec({
    describe("TqTrackingType enum") {
        it("contains only implemented carriers") {
            TqTrackingType.entries.size shouldBe 3
            TqTrackingType.entries shouldBe listOf(TqTrackingType.FedEx, TqTrackingType.UPS, TqTrackingType.USPS)
        }
    }

    describe("FedEx tracking number validation") {
        it("validates FedEx Express tracking numbers") {
            TqTracking.getTrackingType("012345678983") shouldBe TqTrackingType.FedEx
        }
        it("rejects invalid FedEx Express tracking numbers") {
            TqTracking.getTrackingType("012345678984") shouldBe null
            TqTracking.getTrackingType("12345678901") shouldBe null
        }
    }

    describe("UPS tracking number validation") {
        it("validates UPS tracking numbers") {
            TqTracking.getTrackingType("1Z99999999999999994") shouldBe TqTrackingType.UPS
        }
        it("rejects invalid UPS tracking numbers") {
            TqTracking.getTrackingType("999999999999") shouldBe null
            TqTracking.getTrackingType("1Z") shouldBe null
        }
    }

    describe("USPS tracking number validation") {
        it("validates USPS tracking numbers") {
            TqTracking.getTrackingType("94001118992234567890") shouldBe TqTrackingType.USPS
        }
        it("rejects invalid USPS tracking numbers") {
            TqTracking.getTrackingType("123") shouldBe null
        }
    }

    describe("Invalid tracking numbers") {
        it("returns null for empty strings") {
            TqTracking.getTrackingType("") shouldBe null
        }
        it("returns null for random strings") {
            TqTracking.getTrackingType("NOTATRACKINGNUMBER") shouldBe null
            TqTracking.getTrackingType("123456789") shouldBe null
        }
    }
})