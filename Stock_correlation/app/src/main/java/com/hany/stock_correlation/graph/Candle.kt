package com.hany.stock_correlation.graph

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Candle(
    var createdAt: Int,
    var open: Int,
    var close: Int,
    var shadowHigh: Int,
    var shadowLow: Int
                  ): Parcelable
