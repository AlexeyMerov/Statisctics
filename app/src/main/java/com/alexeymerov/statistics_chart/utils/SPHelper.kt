package com.alexeymerov.statistics_chart.utils

import android.content.Context
import android.content.SharedPreferences

object SPHelper {

	lateinit var sharedPrefs: SharedPreferences

	fun init(context: Context, sharedName: String) {
		sharedPrefs = context.getSharedPreferences(sharedName, Context.MODE_PRIVATE)
	}

	inline fun String.onChanged(crossinline valueChanged: (String) -> Unit) {
		sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
			run { if (key == this) valueChanged.invoke(key) }
		}
	}

	inline fun <reified T : Any> getShared(key: String, defValue: T, crossinline observer: (T) -> Unit = {}): T {
		val resultValue = getCurrentValue(key, defValue)
		key.onChanged { observer.invoke(getCurrentValue(key, defValue)) }
		return resultValue
	}

	inline fun <reified T : Any> SharedPreferences.getShared(
			key: String,
			defValue: T,
			crossinline observer: (T) -> Unit = {}
	): T {
		val resultValue = getCurrentValue(key, defValue)
		key.onChanged { observer.invoke(getCurrentValue(key, defValue)) }
		return resultValue
	}

	inline fun <reified T : Any> getCurrentValue(key: String, defValue: T): T =
		sharedPrefs.getCurrentValue(key, defValue)

	inline fun <reified T : Any> SharedPreferences.getCurrentValue(key: String, defValue: T): T {
		val value: Any = run {
			when (defValue) {
				is Boolean -> getBoolean(key, defValue)
				is Long -> getLong(key, defValue)
				is Int -> getInt(key, defValue)
				is String -> getString(key, defValue)
				is Float -> getFloat(key, defValue)
				else -> throw Exception("Cannot cast to any type")
			}
		}
		return value as T
	}

	inline fun <reified T : Any> setShared(key: String, newValue: T) = sharedPrefs.setShared(key, newValue)
	inline fun <reified T : Any> SharedPreferences.setShared(key: String, newValue: T) {
		edit {
			when (newValue) {
				is Long -> putLong(key, newValue)
				is Int -> putInt(key, newValue)
				is String -> putString(key, newValue)
				is Boolean -> putBoolean(key, newValue)
				is Float -> putFloat(key, newValue)
				else -> throw Exception("Cannot cast to any type")
			}
		}
	}

	inline fun SharedPreferences.edit(crossinline action: SharedPreferences.Editor.() -> Unit) {
		edit().apply {
			action(this)
			apply()
		}
	}
}