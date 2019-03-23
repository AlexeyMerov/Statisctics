package com.alexeymerov.statistics_chart.model

data class ChartLine(var number: Int,
					 var isEnabled: Boolean,
					 var dataValues: List<Int>, var name: String, var color: Int
)

data class Popup(val dateString: String, val line: ChartLine, val value: Int, val x: Float, val color: Int)
