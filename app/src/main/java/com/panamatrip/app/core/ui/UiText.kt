package com.panamatrip.app.core.ui

import androidx.annotation.StringRes
import com.panamatrip.app.R

@StringRes
fun UiNotice.noticeTextRes(): Int = when (this) {
    UiNotice.LIKE_UPDATED -> R.string.notice_like_updated
    UiNotice.COMMENT_PUBLISHED -> R.string.notice_comment_published
    UiNotice.SAVED -> R.string.notice_saved
    UiNotice.REMOVED -> R.string.notice_removed
    UiNotice.COLLECTION_CREATED -> R.string.notice_collection_created
    UiNotice.ROUTE_READY -> R.string.notice_route_ready
    UiNotice.ACTION_FAILED -> R.string.notice_action_failed
}

@StringRes
fun UiError.errorTextRes(): Int = when (this) {
    UiError.DATABASE -> R.string.error_body
    UiError.UNKNOWN -> R.string.error_body
}
