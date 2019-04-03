package com.alexeymerov.statistics_chart.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.RawRes
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.model.DateItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChartDataParser {

	private companion object {
		const val DATE_PATTERN = "MMM dd"
		const val DATE_PATTERN_FULL = "EEE, MMM dd"
	}

	private val simpleDateFormat = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())

	fun parseJsonRawRes(context: Context, @RawRes resId: Int,
						onParseComplete: (List<Pair<List<ChartLine>, List<DateItem>>>) -> Unit
	) {
		val allCharts = mutableListOf<Pair<List<ChartLine>, List<DateItem>>>()
		var jsonReader: BufferedReader? = null

		try {
			val jsonBuilder = StringBuilder()
			jsonReader = BufferedReader(InputStreamReader(context.resources.openRawResource(resId)))
			jsonReader.forEachLine { jsonBuilder.append(it) }
			jsonReader.close()

			val jsonArray = JSONArray(JSONTokener(jsonBuilder.toString()))
			for (index in 0 until jsonArray.length()) {
				val chart = jsonArray.getJSONObject(index)
				allCharts.add(parseJson(chart))
			}

		} catch (e: FileNotFoundException) {
			errorLog(e)
		} catch (e: IOException) {
			errorLog(e)
		} catch (e: JSONException) {
			errorLog(e)
		} finally {
			jsonReader?.close()
		}

		onParseComplete(allCharts)
	}

	private fun parseJson(chart: JSONObject): Pair<List<ChartLine>, List<DateItem>> {
		val columns = chart.getJSONArray("columns")
		val names = chart.getJSONObject("names")
		val colors = chart.getJSONObject("colors")
		val chartLines = ArrayList<ChartLine>(colors.length())
		val labelsList = mutableListOf<DateItem>()

		for (columnIndex in 0 until columns.length()) {
			when (columnIndex) {
				0 -> {
					val xValues = columns.getJSONArray(columnIndex)
					for (xValueIndex in 1 until xValues.length()) {
						val dateLong = xValues.get(xValueIndex) as Long

						simpleDateFormat.applyPattern(DATE_PATTERN)
						val date = Date(dateLong)
						val shortDate = simpleDateFormat.format(date)

						simpleDateFormat.applyPattern(DATE_PATTERN_FULL)
						val longDate = simpleDateFormat.format(date)

						labelsList.add(DateItem(shortDate, longDate))
					}
				}
				else -> {
					val yValues = columns.getJSONArray(columnIndex)
					val lineName = names.getString("y${columnIndex - 1}")
					val colorCode = colors.getString("y${columnIndex - 1}")
					val lineValues = ArrayList<Int>(yValues.length() - 1)

					for (yValueIndex in 1 until yValues.length()) {
						lineValues.add(yValues.getInt(yValueIndex))
					}

					chartLines.add(ChartLine(columnIndex, true, lineValues, lineName, Color.parseColor(colorCode)))
				}
			}
		}
		return chartLines to labelsList
	}

}