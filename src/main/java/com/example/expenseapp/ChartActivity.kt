package com.example.expenseapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {

    lateinit var btnMonthYear: TextView
    var selectedMonth = 1
    var selectedYear = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val txtLoading = findViewById<TextView>(R.id.txtLoading)

        btnMonthYear = findViewById(R.id.btnMonthYear)

        pieChart.visibility = View.GONE
        txtLoading.visibility = View.VISIBLE

        val now = Calendar.getInstance()
        selectedMonth = now.get(Calendar.MONTH) + 1
        selectedYear = now.get(Calendar.YEAR)

        updateText()

        btnMonthYear.setOnClickListener {
            showPicker {
                loadData(pieChart, txtLoading)
            }
        }

        // 🔥 FIX NAV FULL
        bottomNav.selectedItemId = R.id.nav_chart

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }

                R.id.nav_chart -> true

                R.id.nav_wallet -> {
                    startActivity(Intent(this, WalletActivity::class.java))
                    true
                }

                R.id.nav_alert -> {
                    startActivity(Intent(this, AlertActivity::class.java))
                    true
                }

                else -> false
            }
        }

        loadData(pieChart, txtLoading)
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

    fun loadData(pieChart: PieChart, txtLoading: TextView) {

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.getExpenses().enqueue(object : Callback<List<Expense>> {

            override fun onResponse(call: Call<List<Expense>>, response: Response<List<Expense>>) {

                val data = response.body() ?: return

                val filteredData = filterData(data, selectedMonth, selectedYear)

                if (filteredData.isEmpty()) {
                    txtLoading.text = "Không có dữ liệu tháng này"
                    txtLoading.visibility = View.VISIBLE
                    pieChart.visibility = View.GONE
                    return
                }

                var income = 0f
                val categoryMap = HashMap<String, Float>()

                for (item in filteredData) {

                    if (item.type == "income") {
                        income += item.amount.toFloat()
                    } else {

                        val key = item.title.lowercase()

                        val category = when {
                            key.contains("ăn") -> "Ăn uống"
                            key.contains("mua") -> "Mua sắm"
                            key.contains("game") -> "Giải trí"
                            else -> "Khác"
                        }

                        val old = categoryMap[category] ?: 0f
                        categoryMap[category] = old + item.amount.toFloat()
                    }
                }

                val entries = ArrayList<PieEntry>()

                if (income > 0) {
                    entries.add(PieEntry(income, "💰 Thu"))
                }

                for ((key, value) in categoryMap) {

                    val label = when (key) {
                        "Ăn uống" -> "🍔 Ăn uống"
                        "Mua sắm" -> "🛍️ Mua sắm"
                        "Giải trí" -> "🎮 Giải trí"
                        else -> "📦 Khác"
                    }

                    entries.add(PieEntry(value, label))
                }

                val dataSet = PieDataSet(entries, "")

                dataSet.colors = listOf(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#FF6384"),
                    Color.parseColor("#FFCE56"),
                    Color.parseColor("#FF9F40"),
                    Color.parseColor("#36A2EB")
                )

                dataSet.valueTextSize = 14f
                dataSet.valueTextColor = Color.WHITE

                val pieData = PieData(dataSet)
                pieData.setValueFormatter(PercentFormatter(pieChart))

                pieChart.data = pieData
                pieChart.setUsePercentValues(true)

                pieChart.setDrawEntryLabels(true)
                pieChart.isDrawHoleEnabled = true
                pieChart.holeRadius = 55f

                pieChart.centerText = "Tổng quan"

                pieChart.description.isEnabled = false
                pieChart.legend.isEnabled = true

                pieChart.animateY(1000)

                txtLoading.visibility = View.GONE
                pieChart.visibility = View.VISIBLE

                pieChart.invalidate()
            }

            override fun onFailure(call: Call<List<Expense>>, t: Throwable) {
                txtLoading.text = "Lỗi kết nối"
            }
        })
    }

    fun filterData(list: List<Expense>, month: Int, year: Int): List<Expense> {

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return list.filter {

            try {
                val date = sdf.parse(it.date) ?: return@filter false

                val cal = Calendar.getInstance()
                cal.time = date

                val itemMonth = cal.get(Calendar.MONTH) + 1
                val itemYear = cal.get(Calendar.YEAR)

                itemMonth == month && itemYear == year

            } catch (e: Exception) {
                false
            }
        }
    }
}