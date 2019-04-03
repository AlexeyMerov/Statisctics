package com.alexeymerov.statistics_chart.model

data class ChartLine(var number: Int,
					 var isEnabled: Boolean,
					 var dataValues: List<Int>, var name: String, var color: Int
)

data class PopupData(val date: DateItem, val line: ChartLine, val value: Int, val x: Float, val color: Int)

data class DateItem(val shortDate: String, val fullDate: String)
