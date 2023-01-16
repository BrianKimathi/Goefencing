package com.example.a_track.models

import android.media.Image
import android.net.Uri

data class User(
    val username: String = "",
    val email: String = "",
    val imageUrl: Uri? = null
)