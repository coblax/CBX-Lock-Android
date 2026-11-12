package com.coblax.examlock.ui.exam

import android.app.AlertDialog
import android.view.WindowManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import android.widget.EditText

/**
 * The exam page's alert, confirm, prompt and leave-page dialogs. WebView's own are titled
 * "The page at https://… says:", and the exam's address is kept from students, so these
 * carry the page's message alone. FLAG_SECURE keeps them out of screenshots like the exam.
 * A dialog that cannot be shown is answered at once, so the page never waits on it.
 *
 * [promptDefault] makes it a prompt with a text field. WebView hands every dialog a
 * [JsPromptResult], so the type of [result] cannot tell a prompt from a confirm.
 */
internal fun WebView?.showExamJsDialog(
    message: String?,
    result: JsResult?,
    withCancel: Boolean,
    isPrompt: Boolean = false,
    promptDefault: String? = null
): Boolean {
    val context = this?.context ?: return false
    if (result == null) return false
    var answered = false
    val input = if (isPrompt) {
        EditText(context).apply {
            setSingleLine()
            setText(promptDefault.orEmpty())
        }
    } else {
        null
    }
    fun answer(confirmed: Boolean) {
        if (answered) return
        answered = true
        when {
            !confirmed -> result.cancel()
            input != null && result is JsPromptResult -> result.confirm(input.text?.toString().orEmpty())
            else -> result.confirm()
        }
    }
    return runCatching {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message.orEmpty())
            .apply { input?.let(::setView) }
            .setPositiveButton(android.R.string.ok) { _, _ -> answer(confirmed = true) }
            .apply {
                if (withCancel) setNegativeButton(android.R.string.cancel) { _, _ -> answer(confirmed = false) }
            }
            // Back or a tap outside: an alert has only OK; the others count as Cancel.
            .setOnDismissListener { answer(confirmed = !withCancel) }
            .create()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.show()
        true
    }.getOrElse {
        answer(confirmed = !withCancel)
        true
    }
}
