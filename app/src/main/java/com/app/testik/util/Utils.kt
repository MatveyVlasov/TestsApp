package com.app.testik.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.app.testik.R
import com.app.testik.data.model.ApiResult
import com.app.testik.domain.model.UserModel
import com.app.testik.util.Constants.USERNAME_GOOGLE_DELIMITER
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

val randomId: String
    get() = UUID.randomUUID().toString()

val timestamp: Long
    get() = System.currentTimeMillis()

fun Int?.orZero() = this ?: 0

fun Long?.orZero() = this ?: 0L

fun String.toIntOrZero() = this.toIntOrNull().orZero()

fun Char.isDigitOrLatinLowercase() = this.isDigit() || this in 'a'..'z'

fun String.isUsername() = all { it.isDigitOrLatinLowercase() } && isNotBlank()

fun String.toUsername() = filter { c -> c.isLetterOrDigit() || c == USERNAME_GOOGLE_DELIMITER }.lowercase()

fun UserModel.getFullName(showUsername: Boolean = true): String {
    var name = if (showUsername) username else ""

    if (firstName.isNotEmpty() && lastName.isNotEmpty()) name += " ($firstName $lastName)"
    else if (firstName.isNotEmpty()) name += " ($firstName)"
    else if (lastName.isNotEmpty()) name += " ($lastName)"

    return name
}

fun String.removeExtraSpaces() = trim().replace("\\s+".toRegex(), " ")

fun String.removeExtraSpacesAndBreaks() = replace("\n", " ").removeExtraSpaces()

fun String.isEmail() = Patterns.EMAIL_ADDRESS.matcher(this).matches() && isNotBlank()

fun Uri?.toAvatar() = toString().takeWhile { it != '=' }

fun String.loadedFromServer() = startsWith("http")

fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Long.toDate(): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(this)
}

fun Long.toTime(): String {
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
        return it.format(this)
    }
}

fun getTimeDifference(start: Long, end: Long): String {
    return (end - start).toTime()
}

fun getHoursAndMinutes(millis: Long): Pair<Int, Int> {
    val hours = millis / 1000 / 60 / 60
    val minutes = (millis / 1000 / 60) % 60

    return Pair(hours.toInt(), minutes.toInt())
}

fun getMillisFromHoursAndMinutes(hours: Int, minutes: Int): Long =
    (hours * 60 + minutes) * 60 * 1000L

fun getStringFromHoursAndMinutes(hours: Int, minutes: Int, resources: Resources): String =
    buildString {
        if (hours == 0 && minutes == 0) {
            append(resources.getString(R.string.no_time_limit))
            return@buildString
        }
        if (hours > 0) {
            append(resources.getQuantityString(R.plurals.hours, hours, hours))
            append(" ")
        }
        if (minutes > 0) {
            append(resources.getQuantityString(R.plurals.minutes, minutes, minutes))
            append(" ")
        }
    }

fun getStringFromMillis(millis: Long, resources: Resources): String {
    val (hours, minutes) = getHoursAndMinutes(millis = millis)
    return getStringFromHoursAndMinutes(hours = hours, minutes = minutes, resources = resources)
}

fun Context.setAppLocale(language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

@ColorInt
fun Context.getThemeColor(@AttrRes attrRes: Int): Int = TypedValue()
    .apply { theme.resolveAttribute (attrRes, this, true) }
    .data

suspend fun<T> Task<T>.execute(): ApiResult<Unit> {
    await()
    return if (isSuccessful) ApiResult.Success()
    else ApiResult.Error(exception?.message)
}

val DocumentReference.private
    get() = collection("private")

fun MaterialToolbar.setupLanguageItem(
    color: Int? = null,
    onClick: () -> Unit = {},
) {
    menu.findItem(R.id.language).apply {
        setActionView(R.layout.item_language)

        actionView?.apply {
            findViewById<TextView>(R.id.tvLanguage).apply {
                text = resources.configuration.locales.get(0).language.uppercase()
                if (color != null) setTextColor(color)
            }
            if (color != null) findViewById<ImageView>(R.id.ivLanguage).setColorFilter(color)
            setOnClickListener { onClick() }
        }
    }
}

fun MaterialToolbar.setupAvatarItem(
    onClick: () -> Unit = {},
) {
    menu.findItem(R.id.profile).apply {
        setActionView(R.layout.item_avatar)

        actionView?.setOnClickListener { onClick() }
    }
}

fun MaterialToolbar.getAvatarItem(): ImageView = menu.findItem(R.id.profile).actionView?.findViewById(R.id.ivAvatar)!!

@SuppressLint("RestrictedApi")
fun MaterialToolbar.showIcons() {
    (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
}

fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false

    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        ?: return false

    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        return true
    }
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        return true
    }
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        return true
    }

    return false
}

fun loadImage(
    context: Context,
    imageView: ImageView,
    url: String,
    @DrawableRes defaultImage: Int,
    isClip: Boolean = true
) {
    val image = url.ifBlank { defaultImage }

    Glide.with(context)
        .load(image)
        .into(imageView)

    imageView.clipToOutline = isClip
}

fun loadAvatar(context: Context, imageView: ImageView, url: String) =
    loadImage(context = context, imageView = imageView, url = url, defaultImage = R.drawable.ic_profile_avatar, isClip = false)

fun loadTestImage(context: Context, imageView: ImageView, url: String) =
    loadImage(context = context, imageView = imageView, url = url, defaultImage = R.drawable.ic_feed)

fun loadQuestionImage(context: Context, imageView: ImageView, url: String) =
    loadImage(context = context, imageView = imageView, url = url, defaultImage = R.drawable.ic_question_mark)

fun TextView.addImage(
    atText: String,
    @DrawableRes image: Int,
    width: Int,
    height: Int,
    onClick: (() -> Unit)? = null
) {
    val ssb = SpannableStringBuilder(text)

    val drawable = ContextCompat.getDrawable(context, image) ?: return
    drawable.mutate()
    drawable.setBounds(0, 0, width, height)

    val start = text.indexOf(atText)
    ssb.setSpan(VerticalImageSpan(drawable), start, start + atText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

    if (onClick != null) {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }
        }
        ssb.setSpan(clickableSpan, start, start + atText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        movementMethod = LinkMovementMethod()
    }

    setText(ssb, TextView.BufferType.SPANNABLE)
}

@SuppressLint("SetTextI18n")
fun TextView.addInfoIcon(onClick: () -> Unit) {
    val tag = "[ic_info]"
    val imageSize = 16.toPx()

    text = "$text $tag"
    addImage(
        atText = tag,
        image = R.drawable.ic_info,
        width = imageSize,
        height = imageSize,
        onClick = onClick
    )
}

fun Int.toABC(): Char {
    val alphabet = ('A'..'Z').toList()
    if (this > alphabet.lastIndex) return ' '
    return alphabet[this]
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

fun getProgressPointsText(pointsEarned: Int, pointsMax: Int, gradeEarned: String): String {
    return gradeEarned.ifEmpty { "${(pointsEarned.toDouble() / pointsMax * 100).toInt()}%" }
}

fun EditText.setupSearchLayout() {
    val params = (layoutParams as ViewGroup.MarginLayoutParams)
    params.updateMargins(left = params.marginStart - 36.toPx())
    updatePadding(left = paddingStart + 28.toPx())
}

fun EditText.setTextIfChanged(text: String) {
    if (this.text.toString() == text) return
    setText(text)
}