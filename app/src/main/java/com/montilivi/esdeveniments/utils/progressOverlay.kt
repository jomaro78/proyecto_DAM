package com.montilivi.esdeveniments.utils

import android.view.View
import androidx.fragment.app.Fragment
import com.montilivi.esdeveniments.R

fun Fragment.toggleProgressOverlay(show: Boolean) {
    view?.findViewById<View>(R.id.progressOverlay)?.visibility =
        if (show) View.VISIBLE else View.GONE
}