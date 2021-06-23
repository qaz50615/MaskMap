package com.lauren.MaskMap

import android.media.MicrophoneInfo
import com.google.android.gms.maps.model.LatLng

data class Geometry(
    var type: String? = null,
    var coordinates: List<Double>? = null
)