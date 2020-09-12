package com.crossbowffs.stfu

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class MainActivity : Activity(), AdapterView.OnItemLongClickListener {
    companion object {
        private const val REQUEST_PERMISSIONS = 1337
        private const val REQUEST_ROLE = 2333
    }

    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private lateinit var filterRuleListView: ListView
    private lateinit var filterRuleAdapter: FilterRuleAdapter
    private var devicePhoneNumber: Phonenumber.PhoneNumber? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filterRuleAdapter = FilterRuleAdapter(this)
        devicePhoneNumber = getDevicePhoneNumber()

        filterRuleListView = findViewById(R.id.filter_rule_list)
        filterRuleListView.emptyView = findViewById(android.R.id.empty)
        filterRuleListView.adapter = filterRuleAdapter
        filterRuleListView.onItemLongClickListener = this

        requestPermissions(arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_PHONE_NUMBERS
        ), REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        if (requestCode != REQUEST_PERMISSIONS) {
            return
        }

        if (grantResults.size != permissions.size ||
            grantResults.any { it != PackageManager.PERMISSION_GRANTED })
        {
            Toast.makeText(this, R.string.phone_permissions_required, Toast.LENGTH_SHORT).show()
            finish()
        }

        requestRole()
    }

    private fun requestRole() {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        startActivityForResult(intent, REQUEST_ROLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_ROLE) {
            return
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.screening_role_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_rule -> {
                showManualEntryDialog()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun getDevicePhoneNumber(): Phonenumber.PhoneNumber? {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val pn = try {
            tm.line1Number
        } catch (e: SecurityException) {
            Klog.e("Failed to get device phone number", e)
            return null
        }

        return try {
            phoneNumberUtil.parse(pn, Locale.getDefault().country)
        } catch (e: NumberParseException) {
            Klog.e("Failed to parse device phone number", e)
            return null
        }
    }

    private fun isDevicePhoneNumberUS(): Boolean {
        val pn = devicePhoneNumber ?: return false
        return phoneNumberUtil.isValidNumberForRegion(pn, "US")
    }

    private fun getNanpDatabase(): NanpDatabase {
        return NanpDatabaseReader(
            CsvReader(
                BufferedReader(
                    InputStreamReader(
                        assets.open("npa_report.csv")
                    )
                )
            )
        ).readAll()
    }

    private fun showManualEntryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_entry, null)
        val editText = dialogView.findViewById(R.id.dialog_manual_entry_edittext) as EditText
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.add_new_filter_rule)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val rule = editText.text.toString()
                filterRuleAdapter.addRules(listOf(rule))
                Toast.makeText(this, getString(R.string.added_rule_fmt, rule), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)

        if (isDevicePhoneNumberUS()) {
            dialogBuilder.setNeutralButton(R.string.add_by_us_area) { _, _ ->
                showSearchByUSAreaDialog()
            }
        }

        val dialog = dialogBuilder.create()

        editText.setText("+${devicePhoneNumber?.countryCode ?: ""}")
        editText.setSelection(editText.length())
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                val isValid = FilterRuleManager.isValidRule(s!!)
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isValid
            }
        })
        editText.requestFocus()

        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun showSearchByUSAreaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_by_area, null)
        val spinner = dialogView.findViewById(R.id.dialog_add_by_area_spinner) as Spinner
        val attribution = dialogView.findViewById(R.id.dialog_add_by_area_attribution) as TextView

        val nanpDatabase = getNanpDatabase()
        val entries = AreaCodeEntry.from(nanpDatabase)
        spinner.adapter = AreaCodeArrayAdapter(this, entries)

        // Try to default to our own number's area code if possible
        val pn = devicePhoneNumber
        if (pn != null) {
            val e164Number = phoneNumberUtil.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164)
            val index = entries.indexOfFirst { entry ->
                entry.areaCodes.any { areaCode ->
                    e164Number.startsWith("+1$areaCode")
                }
            }
            if (index >= 0) {
                spinner.setSelection(index)
            } else {
                Klog.w("Device area code does not match any known area")
            }
        }

        attribution.text = getString(
            R.string.area_data_attribution_fmt,
            nanpDatabase.source,
            nanpDatabase.timestamp
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.add_new_filter_rule)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val rules = entries[spinner.selectedItemPosition].areaCodes.map { areaCode -> "+1$areaCode" }
                val count = filterRuleAdapter.addRules(rules)
                Toast.makeText(this, getString(R.string.added_rules_fmt, count), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.manual_entry) { _, _ ->
                showManualEntryDialog()
            }
            .show()
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        val rule = filterRuleAdapter.getItem(position)
        filterRuleAdapter.deleteByIndex(position)
        Toast.makeText(this, getString(R.string.deleted_rule_fmt, rule), Toast.LENGTH_SHORT).show()
        return true
    }
}
