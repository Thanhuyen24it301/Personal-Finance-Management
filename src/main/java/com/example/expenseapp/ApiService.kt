package com.example.expenseapp

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("expense_app/add_expense.php")
    fun addExpense(@Body expense: Expense): Call<ResponseModel>
    @GET("expense_app/get_expense.php")
    fun getExpenses(): Call<List<Expense>>
    @POST("expense_app/update_expense.php")
    fun updateExpense(@Body expense: Expense): Call<ResponseModel>
    @POST("expense_app/delete_expense.php")
    fun deleteExpense(@Body expense: Expense): Call<ResponseModel>

}

