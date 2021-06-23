package com.lauren.MaskMap

data class FeatureGroup(
    var type: String? = null,
    var properties: Properties? = null,
    var geometry: Geometry? = null
)