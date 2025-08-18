package com.pixelnetica.classloader

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

fun getCurrentYear(): String =
    GregorianCalendar().apply {
        time = Date()
    }.get(Calendar.YEAR).toString()