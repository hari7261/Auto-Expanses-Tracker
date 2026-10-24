package com.example.data.parser

import android.util.Log
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val isCredit: Boolean,
    val merchant: String,
    val accountLast4: String?,
    val paymentMethod: String,
    val confidence: Float
)

object PaymentNotificationParser {
    private const val TAG = "PaymentParser"

    // Patterns for rejecting pure spam, ads, and OTPs to prevent noise
    private val REJECT_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:OTP|verification code|one time password|verification otp)\\b"),
        Pattern.compile("(?i)\\b(?:pre-approved|apply now|get loan|loan offer|cashback up to|win coupon|scratch card)\\b"),
        Pattern.compile("(?i)\\b(?:login alert|successful login|logged in|security alert)\\b")
    )

    // Detailed Amount Extraction (preceded/succeeded by currency indicators)
    private val AMOUNT_PATTERNS = listOf(
        // Matches ₹/Rs/$ prefixed amounts: e.g., "Rs 500", "₹ 150.50", "INR.45", "Rs. 2,000.00"
        Pattern.compile("(?i)(?:rs\\.?|inr|₹|\\$|eur|gbp)\\s*([0-9,]+(?:\\.\\d{1,2})?)"),
        // Matches spent indicator before amount: e.g., "spent: 250", "debited 100"
        Pattern.compile("(?i)(?:spent|paid|debited|credited|received|sent|withdrawn|charged|added)\\s*(?:of\\s+)?([0-9,]+(?:\\.\\d{1,2})?)")
    )

    // Debit signals
    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:debited|spent|paid|sent|withdrawn|transferred|charged|payment|deducted|dr|outgoing)\\b")
    )

    // Credit signals
    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:credited|received|refunded|added|deposited|cr|incoming)\\b")
    )

    // Merchant Patterns - matching in order of priority
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:paid to|sent to|transfer(?:red)? to|towards|credited by|spent on|charged at|info[: ]+|ref[: ]+|vpa\\s+)\\s+([A-Za-z0-9\\s&._#'*-]{2,40})"),
        Pattern.compile("(?i)(?:to|at|from|received from|on|for)\\s+([A-Za-z0-9\\s&._#'*-]{2,40})")
    )

    // Account patterns
    private val ACCT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:a/c|acct|card|xx|ending|bank ac|account)\\s*(?:no|num|in|with)?\\s*([0-9*]*\\d{4})"),
        Pattern.compile("(?i)(?:\\.\\.\\.)\\s*(\\d{4})")
    )

    fun parseNotification(title: String?, message: String?): ParsedTransaction? {
        val titleText = title ?: ""
        val messageText = message ?: ""
        val textToParse = "$titleText $messageText"
        if (textToParse.trim().isEmpty()) return null

        // 1. Filter out spam / OTP / ads (Only if they match spam patterns)
        for (pattern in REJECT_PATTERNS) {
            if (pattern.matcher(textToParse).find()) {
                Log.d(TAG, "Rejected noise/spam/OTP notification: $textToParse")
                return null
            }
        }

        // 2. Main Transactional Keyword Check
        // If the text has no transactional verbs at all, we reject it to avoid normal notification noise
        val isDebit = DEBIT_PATTERNS.any { it.matcher(textToParse).find() }
        val isCredit = CREDIT_PATTERNS.any { it.matcher(textToParse).find() }
        
        if (!isDebit && !isCredit) {
            Log.d(TAG, "No debit/credit signature found. Ignoring.")
            return null
        }

        // 3. Extract amount
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(textToParse)
            if (matcher.find()) {
                val amtStr = matcher.group(1)?.replace(",", "")
                val parsedAmt = amtStr?.toDoubleOrNull()
                if (parsedAmt != null && parsedAmt > 0) {
                    amount = parsedAmt
                    break
                }
            }
        }

        if (amount == null || amount <= 0) {
            Log.d(TAG, "Amount not found or invalid: $textToParse")
            return null
        }

        // 4. Determine if Credit or Debit
        // Default to debit isCredit = false
        val finalIsCredit = isCredit && !isDebit

        // 5. Extract Merchant
        var merchant = ""
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(textToParse)
            if (matcher.find()) {
                val rawMerchant = matcher.group(1)?.trim() ?: ""
                if (rawMerchant.isNotEmpty()) {
                    merchant = rawMerchant
                    break
                }
            }
        }

        // Normalize and clean merchant name
        if (merchant.isNotEmpty()) {
            // Trim UPI @ handle extension if exists (e.g. Swiggy@okaxis -> Swiggy)
            if (merchant.contains("@")) {
                merchant = merchant.split("@")[0].trim()
            }

            // Remove common starting prefixes like "UPI-" or "VPA-" or "UPI "
            merchant = merchant.replace(Regex("(?i)^(?:upi|vpa)[-*\\s]+"), "").trim()

            val stopWords = listOf(
                " using", " on ", " for ", " active", " via", " at ", " in ", " with", " towards", 
                " through", " ref", " upi", " txn", " on date", " dt", " successfully", " card", " limit", " ending",
                " from ", " acct", " a/c", " account", " bank", " sent", " paid", " received", " charged", " ending"
            )
            var lowerMerchant = merchant.lowercase()
            for (stopWord in stopWords) {
                val index = lowerMerchant.indexOf(stopWord)
                if (index != -1) {
                    merchant = merchant.substring(0, index).trim()
                    lowerMerchant = merchant.lowercase()
                }
            }
            
            // Trim any trailing non-word noise characters like commas, dashes, dots, stars
            merchant = merchant.replace(Regex("^[\\s._#*-]+|[\\s._#*-]+$"), "").trim()

            // If the extracted merchant is an auxiliary/helper word, reset
            val badWords = setOf("your", "my", "our", "a/c", "account", "bank", "the", "an", "is", "of", "rs", "inr")
            if (badWords.contains(merchant.lowercase()) || merchant.length < 2) {
                merchant = ""
            }
        }

        // Fallback merchant based on context or source app
        if (merchant.isEmpty()) {
            merchant = when {
                titleText.isNotEmpty() && !titleText.contains("Notification", ignoreCase = true) -> titleText
                else -> "Transaction"
            }
        }

        // 6. Extract account ending
        var accountLast4: String? = null
        for (pattern in ACCT_PATTERNS) {
            val matcher = pattern.matcher(textToParse)
            if (matcher.find()) {
                val fullAcctStr = matcher.group(1) ?: ""
                if (fullAcctStr.length >= 4) {
                    accountLast4 = fullAcctStr.takeLast(4)
                    break
                }
            }
        }

        // 7. Payment method classification
        var paymentMethod = "UPI"
        val lowerText = textToParse.lowercase()
        when {
            lowerText.contains("card") || lowerText.contains("debit card") || lowerText.contains("credit card") || lowerText.contains("visa") || lowerText.contains("mastercard") || lowerText.contains("cc") -> {
                paymentMethod = "Card"
            }
            lowerText.contains("netbanking") || lowerText.contains("net banking") || lowerText.contains("online banking") || lowerText.contains("imps") || lowerText.contains("neft") || lowerText.contains("rtgs") -> {
                paymentMethod = "Netbanking"
            }
            lowerText.contains("atm") || lowerText.contains("cash withdrawal") -> {
                paymentMethod = "ATM"
            }
        }

        // 8. Confidence scoring
        var confidence = 0.6f
        if (isDebit || isCredit) confidence += 0.2f
        if (accountLast4 != null) confidence += 0.1f
        if (merchant != "Transaction") confidence += 0.1f
        confidence = confidence.coerceAtMost(1.0f)

        return ParsedTransaction(
            amount = amount,
            isCredit = finalIsCredit,
            merchant = merchant,
            accountLast4 = accountLast4,
            paymentMethod = paymentMethod,
            confidence = confidence
        )
    }
}
