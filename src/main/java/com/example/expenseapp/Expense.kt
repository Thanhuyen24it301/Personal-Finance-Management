package com.example.expenseapp

data class Expense(
    val id: Int,
    val title: String,
    var amount: Double,   // ⚠️ đổi val → var
    val type: String,
    val date: String
)