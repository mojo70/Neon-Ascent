package com.neon.ascent.feature.biohacking.ui.labs

/** Tape samples are stored in centimeters. Imperial entry and display use inches. */
const val CENTIMETERS_PER_INCH = 2.54

fun tapeEntryToCentimeters(entered: Double, imperial: Boolean): Double =
    if (imperial) entered * CENTIMETERS_PER_INCH else entered

fun waistDisplayValue(storedCentimeters: Double, imperial: Boolean): Double =
    if (imperial) storedCentimeters / CENTIMETERS_PER_INCH else storedCentimeters
