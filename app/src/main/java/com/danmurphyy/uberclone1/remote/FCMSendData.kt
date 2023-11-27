package com.danmurphyy.uberclone1.remote

data class FCMSendData(
    var to: String,
    var data: Map<String, String>,
)