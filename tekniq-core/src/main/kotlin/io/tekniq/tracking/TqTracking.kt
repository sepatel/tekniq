package io.tekniq.tracking

enum class TqTrackingType {
    Airborne, AustraliaPost, CanadaPost, DHL, FedEx, TNT, UPS, USPS
}

object TqTracking {
    fun getTrackingType(trackingNumber: String): TqTrackingType? {
        if (isFedExExpress(trackingNumber) || isFedExGround(trackingNumber)) {
            return TqTrackingType.FedEx
        } else if (isUps(trackingNumber)) {
            return TqTrackingType.UPS
            //} else if (isAirborne(trackingNumber)) {
            //return TqTrackingType.Airborne
        } else if (isUsps(trackingNumber)) {
            return TqTrackingType.USPS
        }
        return null
    }

    private fun differenceFromNextHighestMultipleOf10(sum: Int): Int {
        return (sum / 10 * 10 + 10 - sum) % 10
    }

    /**
     *
     * Airborne Express utilizes the stanard MOD 7 method for their tracking
     * numbers.  The check digit is the last digit of the tracking number.

     * [208-914-2901 x Val in Boise Call Center]
     *
     */
    /* private fun isAirborne(trackingNumber: String): Boolean {
        return false
    } */

    /**
     *
     * EXPRESS SHIPMENTS:

     * For example, using tracking number: 012345678983

     * Take the first 11 digits of tracking number.  Starting with the 11th
     * position, take the digits 1, 3, and 7, and assign them to each digit
     * repeatedly.

     * 012345678983
     * |||||||||||
     * 31731731731

     * Multiply each assigned number to its corresponding tracking number
     * digit:

     * 0 1 14 9 4 35 18 7 56 27 8

     * Add the products together [= 179 in this instance]

     * Divide the sum by 11.  You get 16 remainder 3.If the remainder is 10,
     * then the check digit is 0.  If there is no remainder, the check digit
     * is 0.  The remainder is the check digit and should equal the 12th
     * digit of the tracking number.
     *
     */
    private fun isFedExExpress(trackingNumber: String): Boolean {
        if (trackingNumber.length != 12) {
            return false
        }
        val hash = intArrayOf(3, 1, 7)
        var sum = 0
        for (i in 0..10) {
            val digit = trackingNumber[i] - '0'
            if (digit < 0 || digit > 9) {
                return false
            }
            sum += digit * hash[i % hash.size]
        }
        var remainder = sum % 11
        if (remainder == 10) {
            remainder = 0
        }

        return trackingNumber[11] - '0' == remainder
    }

    /**
     *
     * FOR GROUND SHIPMENTS:

     * See this PDF file on the web:

     * grd.fedex.com/online/mcode/fedex_ground_label_layout_specification.pdf

     * under Check Digit Calculation Algorithms section

     * [Web API dept. - 800-810-9073 [option 1]]
     * [Bruce Clark                            ]
     * [CASE NUMBER: 11016731                  ]

     * --

     * Positions are from **Right** to Left
     * Step 1. Starting from position 2, add up the values of the even numbered positions.
     * Step 2. Multiply the results of step Step 1. By three.
     * Step 3. Starting from position 3, add up the values of the odd numbered positions. Remember – position 1 is the
     * check digit you are trying to calculate.
     * Step 4. Add the result of step Step 2. To the result of step Step 3.
     * Step 5. Determine the smallest number which when added to the number from Step 4. Results in a multiple of 10.
     * This is the check digit.
     *
     */
    private fun isFedExGround(trackingNumber: String): Boolean {
        var tracking = trackingNumber.replace(" ".toRegex(), "") // remove all whitespace
        if (tracking.length == 22) { // 96 information ground shipment
            if (!tracking.startsWith("96")) {
                return false
            }
            tracking = tracking.substring(7) // strip the 96 information
        }

        if (tracking.length != 15) { // not a valid tracking number
            return false
        }

        val checkDigit = tracking[tracking.length - 1] - '0'
        if (checkDigit < 0 || checkDigit > 9) {
            return false
        }

        var sum = 0
        for (i in 0 until tracking.length - 1) {
            val digit = tracking[i] - '0'
            if (digit < 0 || digit > 9) {
                return false
            }
            sum += if (i % 2 == 0) digit else digit * 3
        }

        val calcCheckDigit = differenceFromNextHighestMultipleOf10(sum)

        return checkDigit == calcCheckDigit
    }

    /**
     *
     * The 1Z tracking numbers utilize a modified MOD 10 calculation.

     * 1. Exclude 1Z data identifier from calculation.
     * 2. Convert all alpha characters to their numeric equivalents using
     * chart below.
     * 3. From left, add all odd positions.
     * 4. From left, add all even positions and multiply by two.
     * 5. Add results of steps 3 and 4.
     * 6. Subtract result from next highest multiple of 10.
     * 7. The remainder is your check digit [the last digit].

     * Note: If the remainder is 10, the check digit is 0.Alpha to numeric
     * cross reference chart

     * A=2
     * B=3
     * C=4
     * D=5
     * E=6
     * F=7
     * G=8
     * H=9
     * I=0
     * J=1
     * K=2
     * L=3
     * M=4
     * N=5
     * O=6
     * p=7
     * Q=8
     * R=9
     * S=0
     * T=1
     * U=2
     * V=3
     * W=4
     * X=5
     * Y=6
     * Z=7

     * ---

     * For all other tracking numbers, the standard MOD 10 algorithm applies
     * for the 11th check-digit.

     * [Business Development Dept.                      ]
     * [404-828-6627 x Getty Gidash                     ]
     * [Mark Lewis is a name that Getty may refer you to]
     *

     *
     * Additional Reference:
     * http://www.codeproject.com/Articles/21224/Calculating-the-UPS-Tracking-Number-Check-Digit?fid=479913&df=90&mpp=25&noise=3&prof=False&sort=Position&view=Quick&spc=Relaxed&fr=8

     * 1.  The first two characters must be "1Z".
     * 2.  The next 6 characters we fill with our UPS account number "XXXXXX".
     * 3.  The next 2 characters denote the service type:
     * a.  "01" for Next Day Air shipments.
     * b.  "02" for Second Day Air shipments.
     * c.  "03" for Ground shipments.
     * 4.  The next 5 characters is our invoice number (our invoices are 6 digits; we drop the first digit, e.g., the 123456 invoice would yield 23456 characters).
     * 5.  The next 2 digits is the package number, zero filled. E.g., package 1 is "01", 2 is "02".
     * 6.  The last and final character is the check digit.

     *

     *
     * Additional Reference: http://www.ups.com/content/us/en/tracking/help/tracking/tnh.html
     * UPS tracking numbers appear in the following formats:
     * 1Z9999999999999999
     * 999999999999
     * T9999999999
     * 999999999
     *
     */
    private fun isUps(trackingNumber: String): Boolean {
        val tracking = trackingNumber.toUpperCase().replace(" ".toRegex(), "")
        if (!tracking.startsWith("1Z")) {
            return false
        }

        var sum = 0
        for (i in 2 until tracking.length - 1) {
            val digit = tracking[i]
            var value = digit - '0'
            if (value > 9) { // need to convert letters to numbers accordingly
                value = (digit - 'A' + 2) % 10
            }
            sum += if (i % 2 == 0) value else value * 2
        }

        val checkdigit = differenceFromNextHighestMultipleOf10(sum)
        return tracking[tracking.length - 1] - '0' == checkdigit
    }

    /**
     *
     * Please see the following publications for check digit information.
     * Note: “PIC” is their term for “tracking number.”  In addition to the
     * specific sections/page numbers below, I would advise searching for
     * “check digit” in these documents.

     * Publication 91 – Delivery and Signature Confirmation numbers
     * Acrobat page 85 [literally page 79]
     * http://www.usps.com/cpim/ftp/pubs/pub91.pdf

     * Publication 97 – Express Mail Manifesting Technical Guide
     * Acrobat page 57 [literally page 59]
     * http://www.usps.com/cpim/ftp/pubs/pub97.pdf

     * Publication 109 – Special Services Technical GuideSection 7.6.3
     * http://www.usps.com/cpim/ftp/pubs/pub109.pdf
     * [Charles in Delivery Confirmation: 877-264-9693]
     * [He only has information on the Delivery       ]
     * [and signature confirmation schemes            ]


     * Additional link:
     * Mod information
     * http://www.formtechservices.com/dstuff/bookstuf/modnos.html
     *
     */
    private fun isUsps(trackingNumber: String): Boolean {
        var tracking = trackingNumber.toUpperCase().replace(" ".toRegex(), "")
        val length = tracking.length
        if (length != 22 && length != 20 && length != 13) {
            return false
        }
        if (length == 13) {
            if (!tracking.endsWith("US")) {
                return false
            }
            tracking = tracking.substring(2, 11)
            // mod 11 check which if it fails drop back to mod 10 check as either are valid for express
            val multipliers = intArrayOf(8, 6, 4, 2, 3, 5, 9, 7)
            var sum = 0
            for (i in 0 until tracking.length - 1) {
                val value = tracking[i] - '0'
                sum += value * multipliers[i]
            }
            val checkdigit: Int = when (val remainder = sum % 11) {
                0 -> 5
                1 -> 0
                else -> 11 - remainder
            }
            if (tracking[tracking.length - 1] - '0' == checkdigit) {
                return true
            }
        }

        var sum = 0
        for (i in tracking.length - 2 downTo 0) {
            val value = tracking[i] - '0'
            sum += if (i % 2 == tracking.length % 2) 3 * value else value
        }

        val checkdigit = differenceFromNextHighestMultipleOf10(sum)
        return tracking[tracking.length - 1] - '0' == checkdigit
    }
}
