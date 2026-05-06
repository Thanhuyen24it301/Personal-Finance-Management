package com.example.expenseapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    lateinit var api: ApiService
    val dataList = ArrayList<Expense>()
    lateinit var adapter: ArrayAdapter<Expense>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val txtIncome = findViewById<TextView>(R.id.txtIncome)
        val txtExpense = findViewById<TextView>(R.id.txtExpense)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val listView = findViewById<ListView>(R.id.listView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_home

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)

        adapter = object : ArrayAdapter<Expense>(this, R.layout.item_expense, dataList) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view = layoutInflater.inflate(R.layout.item_expense, parent, false)

                val txt = view.findViewById<TextView>(R.id.txtItem)
                val btnDelete = view.findViewById<ImageView>(R.id.btnDelete)
                val btnEdit = view.findViewById<ImageView>(R.id.btnEdit)

                val item = getItem(position)

                val format = DecimalFormat("#,###")
                val money = format.format(item?.amount ?: 0).replace(",", ".")
                val icon = if (item?.type == "income") "📥" else "📤"

                txt.text = "$icon ${item?.title} - $money đ"

                // DELETE
                btnDelete.setOnClickListener {
                    item?.let {
                        api.deleteExpense(it).enqueue(object : Callback<ResponseModel> {
                            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                                Toast.makeText(this@HomeActivity, "Đã xoá", Toast.LENGTH_SHORT).show()
                                loadData()
                            }

                            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                                Toast.makeText(this@HomeActivity, "Lỗi", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }

                // 🔥 EDIT = DIALOG GIỐNG ADD
                btnEdit.setOnClickListener {

                    if (item == null) return@setOnClickListener

                    val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)

                    val edtTitle = dialogView.findViewById<EditText>(R.id.edtTitle)
                    val edtMoney = dialogView.findViewById<EditText>(R.id.edtMoney)
                    val spinner = dialogView.findViewById<Spinner>(R.id.spinnerType)

                    edtTitle.setText(item.title)
                    edtMoney.setText(item.amount.toInt().toString())

                    val types = arrayOf("Chi tiêu", "Thu nhập")
                    spinner.adapter = ArrayAdapter(this@HomeActivity, android.R.layout.simple_spinner_dropdown_item, types)

                    spinner.setSelection(if (item.type == "expense") 0 else 1)

                    val dialog = android.app.AlertDialog.Builder(this@HomeActivity)
                        .setTitle("✏️ Sửa giao dịch")
                        .setView(dialogView)
                        .setPositiveButton("Cập nhật", null)
                        .setNegativeButton("Hủy", null)
                        .create()

                    dialog.show()

                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                        val title = edtTitle.text.toString()
                        val money = edtMoney.text.toString().toDoubleOrNull()

                        if (title.isEmpty() || money == null) {
                            Toast.makeText(this@HomeActivity, "Nhập thiếu!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val type = if (spinner.selectedItemPosition == 0) "expense" else "income"

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val currentDate = sdf.format(Date())

                        val updated = Expense(item.id, title, money, type, currentDate)

                        api.updateExpense(updated).enqueue(object : Callback<ResponseModel> {
                            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                                Toast.makeText(this@HomeActivity, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                                loadData()
                                dialog.dismiss()
                            }

                            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                                Toast.makeText(this@HomeActivity, "Lỗi", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }

                return view
            }
        }

        listView.adapter = adapter
        loadData()

        // ADD (GIỮ NGUYÊN)
        btnAdd.setOnClickListener {

            val view = layoutInflater.inflate(R.layout.dialog_add, null)

            val edtTitle = view.findViewById<EditText>(R.id.edtTitle)
            val edtMoney = view.findViewById<EditText>(R.id.edtMoney)
            val spinner = view.findViewById<Spinner>(R.id.spinnerType)

            edtMoney.inputType = InputType.TYPE_CLASS_NUMBER

            val types = arrayOf("Chi tiêu", "Thu nhập")
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

            val dialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Thêm", null)
                .setNegativeButton("Huỷ", null)
                .create()

            dialog.show()

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val title = edtTitle.text.toString()
                val money = edtMoney.text.toString().toDoubleOrNull()

                if (title.isEmpty() || money == null) {
                    Toast.makeText(this, "Nhập thiếu!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val type = if (spinner.selectedItemPosition == 0) "expense" else "income"

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = sdf.format(Date())

                val newExpense = Expense(0, title, money, type, currentDate)

                api.addExpense(newExpense).enqueue(object : Callback<ResponseModel> {
                    override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                        Toast.makeText(this@HomeActivity, "Đã thêm", Toast.LENGTH_SHORT).show()
                        loadData()
                        dialog.dismiss()
                    }

                    override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                        Toast.makeText(this@HomeActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> true

                R.id.nav_chart -> {
                    startActivity(Intent(this, ChartActivity::class.java))
                    true
                }

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
    }

    fun loadData() {
        api.getExpenses().enqueue(object : Callback<List<Expense>> {

            override fun onResponse(call: Call<List<Expense>>, response: Response<List<Expense>>) {

                val data = response.body() ?: return

                dataList.clear()

                var income = 0.0
                var expense = 0.0

                for (item in data) {
                    dataList.add(item)
                    if (item.type == "income") income += item.amount
                    else expense += item.amount
                }

                val format = DecimalFormat("#,###")

                val incomeStr = format.format(income).replace(",", ".")
                val expenseStr = format.format(expense).replace(",", ".")

                findViewById<TextView>(R.id.txtIncome).text = "$incomeStr đ"
                findViewById<TextView>(R.id.txtExpense).text = "$expenseStr đ"

                adapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<List<Expense>>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Load lỗi", Toast.LENGTH_SHORT).show()
            }
        })
    }
}