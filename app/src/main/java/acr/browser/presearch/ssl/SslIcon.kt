package acr.browser.presearch.ssl

import acr.browser.presearch.R
import acr.browser.presearch.utils.DrawableUtils
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Creates the proper [Drawable] to represent the [SslState].
 */
fun Context.createSslDrawableForState(sslState: SslState): Drawable? = when (sslState) {
    is SslState.None -> null
    is SslState.Valid -> {
        val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_secured)
        val securedDrawable = BitmapDrawable(resources, bitmap)
        securedDrawable
    }
    is SslState.Invalid -> {
        val bitmap = DrawableUtils.createImageInsetInRoundedSquare(this, R.drawable.ic_unsecured)
        val unsecuredDrawable = BitmapDrawable(resources, bitmap)
        unsecuredDrawable
    }
}
