package com.example.expenseapp

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity() {

    lateinit var api: ApiService
    var id = -1
    var currentType = "expense" // 🔥 giữ lại type cũ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val edtTitle = EditText(this)
        edtTitle.hint = "Tên"

        val edtMoney = EditText(this)
        edtMoney.hint = "Số tiền"
        edtMoney.inputType = InputType.TYPE_CLASS_NUMBER

        val btnSave = Button(this)
        btnSave.text = "Lưu"

        layout.addView(edtTitle)
        layout.addView(edtMoney)
        layout.addView(btnSave)

        setContentView(layout)

        id = intent.getIntExtra("id", -1)

        // 🔥 check id
        if (id == -1) {
            Toast.makeText(this, "Lỗi ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)

        // 🔥 LOAD DATA CŨ
        api.getExpenses().enqueue(object : Callback<List<Expense>> {
            override fun onResponse(call: Call<List<Expense>>, response: Response<List<Expense>>) {

                val data = response.body() ?: return

                for (item in data) {
                    if (item.id == id) {
                        edtTitle.setText(item.title)
                        edtMoney.setText(item.amount.toInt().toString())
                        currentType = item.type // 🔥 giữ type
                        break
                    }
                }
            }

            override fun onFailure(call: Call<List<Expense>>, t: Throwable) {
                Toast.makeText(this@EditActivity, "Lỗi load", Toast.LENGTH_SHORT).show()
            }
        })

        // 🔥 SAVE
        btnSave.setOnClickListener {

            val title = edtTitle.text.toString()
            val money = edtMoney.text.toString().toDoubleOrNull()

            if (title.isEmpty() || money == null) {
                Toast.makeText(this, "Nhập thiếu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updated = Expense(
                id,
                title,
                money,
                currentType, // 🔥 giữ type cũ
                getCurrentDate() // 🔥 ngày hiện tại
            )

            api.updateExpense(updated).enqueue(object : Callback<ResponseModel> {
                override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                    Toast.makeText(this@EditActivity, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                    Toast.makeText(this@EditActivity, "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 🔥 lấy ngày hiện tại
    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}