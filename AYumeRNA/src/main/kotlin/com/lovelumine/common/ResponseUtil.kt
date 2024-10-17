package com.lovelumine.common

import java.util.Date

object ResponseUtil {
    fun formatResponse(code: Int, data: Any): Map<String, Any> {
        return mapOf(
            "code" to code,
            "data" to data,
            "timestamp" to Date()
        )
    }
}
