package com.example.expenseapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class AlertActivity : AppCompatActivity() {

    lateinit var btnMonthYear: TextView
    var selectedMonth = 1
    var selectedYear = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        val listView = findViewById<ListView>(R.id.listView)
        val txtEmpty = findViewById<TextView>(R.id.txtEmpty)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        btnMonthYear = findViewById(R.id.btnMonthYear)

        val now = Calendar.getInstance()
        selectedMonth = now.get(Calendar.MONTH) + 1
        selectedYear = now.get(Calendar.YEAR)

        updateText()

        btnMonthYear.setOnClickListener {
            showPicker {
                loadData(listView, txtEmpty)
            }
        }

        bottomNav.selectedItemId = R.id.nav_alert

        loadData(listView, txtEmpty)

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_chart -> {
                    startActivity(Intent(this, ChartActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_wallet -> {
                    startActivity(Intent(this, WalletActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_alert -> true

                else -> false
            }
        }
    }

    fun updateText() {
        btnMonthYear.text = "Tháng $selectedMonth / $selectedYear ▼"
    }

    fun showPicker(onSelected: () -> Unit) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_picker)

        val spMonth = dialog.findViewById<Spinner>(R.id.spMonth)
        val spYear = dialog.findViewById<Spinner>(R.id.spYear)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        val months = (1..12).map { "Tháng $it" }
        val years = (2020..2030).map { "$it" }

        spMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)

        spMonth.setSelection(selectedMonth - 1)
        spYear.setSelection(years.indexOf(selectedYear.toString()))

        btnOk.setOnClickListener {
            selectedMonth = spMonth.selectedItemPosition + 1
            selectedYear = spYear.selectedItem.toString().toInt()

            updateText()
            onSelected()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadData(listView: ListView, txtEmpty: TextView) {

        // 🔥 LỌC ALERT THEO THÁNG
        val data = AlertManager.alerts.filter {
            it.time.contains("$selectedMonth/$selectedYear")
        }.map {
            "⏰ ${it.time} - ${it.message}"
        }

        if (data.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            data
        )

        listView.adapter = adapter
    }
}