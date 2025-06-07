import org.gradle.api.Project
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

fun Project.getCurrentYear(): String =
    GregorianCalendar().apply {
        time = Date()
    }.get(Calendar.YEAR).toString()