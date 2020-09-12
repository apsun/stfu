package com.crossbowffs.stfu

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.*

class ScreeningService : CallScreeningService() {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private lateinit var filterRuleManager: FilterRuleManager

    override fun onCreate() {
        super.onCreate()
        filterRuleManager = FilterRuleManager(this)
    }

    private fun normalizeE164(number: String): String {
        // Fairly sure this is not the right way to get the country code,
        // but looking at the Android source code many places do it this way,
        // so it surely can't be that far off.
        val pn = try {
            phoneNumberUtil.parse(number, Locale.getDefault().country)
        } catch (e: NumberParseException) {
            return number
        }
        return phoneNumberUtil.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164)
    }

    private fun blockCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        Klog.i("Screening call handle=${callDetails.handle}")

        // Only look at incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            Klog.i("Call is not inbound, ignoring")
            return
        }

        // Block all restricted numbers
        if (callDetails.handlePresentation == TelecomManager.PRESENTATION_RESTRICTED) {
            Klog.i("Call is from a restricted number, blocking")
            return blockCall(callDetails)
        }

        // Check against the filter rules
        val number = callDetails.handle.schemeSpecificPart
        val e164Number = normalizeE164(number)
        return if (filterRuleManager.isBlocked(e164Number)) {
            Klog.i("Screening result for $e164Number -> blocked")
            blockCall(callDetails)
        } else {
            Klog.i("Screening result for $e164Number -> allowed")
            allowCall(callDetails)
        }
    }
}
