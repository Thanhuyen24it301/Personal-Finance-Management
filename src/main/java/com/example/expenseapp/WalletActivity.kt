package com.example.expenseapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class WalletActivity : AppCompatActivity() {

    var limit = 2000000.0
    lateinit var btnMonthYear: TextView

    var selectedMonth = 1
    var selectedYear = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val tvLimit = findViewById<TextView>(R.id.tvLimit)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        btnMonthYear = findViewById(R.id.btnMonthYear)

        val now = Calendar.getInstance()
        selectedMonth = now.get(Calendar.MONTH) + 1
        selectedYear = now.get(Calendar.YEAR)

        updateText()

        btnMonthYear.setOnClickListener {
            showPicker {
                loadData(tvBalance, tvLimit, progress)
            }
        }

        bottomNav.selectedItemId = R.id.nav_wallet

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_chart -> { startActivity(Intent(this, ChartActivity::class.java)); true }
                R.id.nav_wallet -> true
                R.id.nav_alert -> { startActivity(Intent(this, AlertActivity::class.java)); true }
                else -> false
            }
        }

        loadData(tvBalance, tvLimit, progress)
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

    fun loadData(tvBalance: TextView, tvLimit: TextView, progress: ProgressBar) {

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.getExpenses().enqueue(object : Callback<List<Expense>> {

            override fun onResponse(call: Call<List<Expense>>, response: Response<List<Expense>>) {

                val data = response.body() ?: return

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                var income = 0.0
                var expense = 0.0

                for (i in data) {
                    try {
                        val date = sdf.parse(i.date) ?: continue

                        val cal = Calendar.getInstance()
                        cal.time = date

                        val m = cal.get(Calendar.MONTH) + 1
                        val y = cal.get(Calendar.YEAR)

                        if (m == selectedMonth && y == selectedYear) {
                            if (i.type == "income") income += i.amount
                            else expense += i.amount
                        }

                    } catch (e: Exception) {}
                }

                val balance = income - expense

                // 🔥 FIX LỖI KOTLIN
                val percent = if (limit == 0.0) 0.0 else (expense / limit * 100)

                val format = DecimalFormat("#,###")
                val balanceStr = format.format(balance).replace(",", ".")

                tvBalance.text = "💰 Số dư: $balanceStr đ"
                tvLimit.text = "Đã dùng: ${percent.toInt()}%"

                progress.progress = percent.toInt().coerceAtMost(100)

                // 🔥 màu progress
                val color = when {
                    percent < 50.0 -> Color.GREEN
                    percent < 100.0 -> Color.YELLOW
                    else -> Color.RED
                }
                progress.progressTintList = android.content.res.ColorStateList.valueOf(color)

                // 🔥 ALERT
                AlertManager.alerts.clear()

                val time = "Tháng $selectedMonth/$selectedYear"

                when {
                    percent >= 100 -> AlertManager.alerts.add(Alert("❌ Vượt hạn mức!", time))
                    percent >= 70 -> AlertManager.alerts.add(Alert("⚠️ Cảnh báo ${percent.toInt()}%", time))
                    percent >= 50 -> AlertManager.alerts.add(Alert("🔔 Nhắc nhở ${percent.toInt()}%", time))
                }
            }

            override fun onFailure(call: Call<List<Expense>>, t: Throwable) {}
        })
    }
}