package com.coblax.examlock.ui.exam

import com.coblax.examlock.config.ExamAppKeyHeaderName
import java.net.URI
import java.util.Locale

/**
 * The X-CBX-App-Key header for one exam, scoped to the exam's own host so the key never
 * goes to a third-party site the exam links to.
 *
 * WebView has no way to add a header to every request, so it is sent on three paths:
 *  - pages the app loads itself (the exam link, retries, Refresh): [SecureExamWebView.loadUrl];
 *  - links and redirects inside the exam (GET): re-issued with the header from
 *    shouldOverrideUrlLoading;
 *  - fetch and XMLHttpRequest calls of the exam page: [buildExamAppKeyScript].
 * A plain HTML form POST cannot carry it; WebView sends those as they are.
 */
internal data class ExamHostHeaders(
    val scheme: String,
    val host: String,
    val port: Int,
    val headers: Map<String, String>
) {
    val headerName: String get() = headers.keys.first()
    val headerValue: String get() = headers.values.first()

    fun appliesTo(url: String?): Boolean {
        val uri = url?.let { runCatching { URI(it.trim()) }.getOrNull() } ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "http" && scheme != "https") return false
        return uri.host?.lowercase(Locale.ROOT) == host
    }

    /** WebView origin rules for the document-start script: the exam origin, both schemes. */
    fun originRules(): Set<String> {
        val portSuffix = if (port > 0) ":$port" else ""
        return linkedSetOf("$scheme://$host$portSuffix", "https://$host", "http://$host")
    }
}

internal fun buildExamHostHeaders(examUrl: String, appKey: String): ExamHostHeaders? {
    val key = appKey.trim()
    if (key.isEmpty()) return null
    val uri = runCatching { URI(examUrl.trim()) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase(Locale.ROOT)?.takeIf { it == "http" || it == "https" }
        ?: return null
    val host = uri.host?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return null
    return ExamHostHeaders(
        scheme = scheme,
        host = host,
        port = uri.port,
        headers = mapOf(ExamAppKeyHeaderName to key)
    )
}

/** True when a request already carries the header (WebView reports it in any case). */
internal fun Map<String, String>?.hasHeader(name: String): Boolean =
    this?.keys?.any { it.equals(name, ignoreCase = true) } == true

/**
 * Adds the header to the exam page's own fetch and XMLHttpRequest calls. Same-origin
 * only: a custom header on a cross-origin call would trigger a CORS preflight the other
 * server may refuse. ES5, for the outdated WebViews on Android 7.
 */
internal fun buildExamAppKeyScript(headers: ExamHostHeaders): String {
    val name = jsString(headers.headerName)
    val value = jsString(headers.headerValue)
    return """
(function() {
  if (window.__cbxAppKeyInstalled) return;
  window.__cbxAppKeyInstalled = true;
  var NAME = $name, VALUE = $value;
  function sameOrigin(url) {
    try {
      var a = document.createElement('a');
      a.href = url;
      return a.protocol === location.protocol && a.host === location.host;
    } catch (e) { return false; }
  }
  var XHR = window.XMLHttpRequest;
  if (XHR && XHR.prototype) {
    var open = XHR.prototype.open, send = XHR.prototype.send;
    XHR.prototype.open = function(method, url) {
      this.__cbxSameOrigin = sameOrigin(url);
      return open.apply(this, arguments);
    };
    XHR.prototype.send = function() {
      if (this.__cbxSameOrigin) { try { this.setRequestHeader(NAME, VALUE); } catch (e) {} }
      return send.apply(this, arguments);
    };
  }
  if (window.fetch && window.Headers) {
    var nativeFetch = window.fetch;
    window.fetch = function(input, init) {
      try {
        var isRequest = typeof input !== 'string' && input && input.url;
        var url = isRequest ? input.url : String(input);
        if (sameOrigin(url)) {
          var copy = {}, key;
          if (init) { for (key in init) { copy[key] = init[key]; } }
          var headers = new Headers(copy.headers || (isRequest ? input.headers : undefined));
          if (!headers.has(NAME)) headers.set(NAME, VALUE);
          copy.headers = headers;
          return nativeFetch.call(window, input, copy);
        }
      } catch (e) {}
      return nativeFetch.apply(window, arguments);
    };
  }
})();
""".trimIndent()
}

/** A JavaScript string literal; the key is admin input and must not break out of it. */
private fun jsString(value: String): String = buildString {
    append('\'')
    value.forEach { char ->
        when {
            char == '\\' -> append("\\\\")
            char == '\'' -> append("\\'")
            char == '<' -> append("\\x3c")
            char.code < 0x20 || char.code > 0x7e ->
                append("\\u").append(String.format(Locale.ROOT, "%04x", char.code))
            else -> append(char)
        }
    }
    append('\'')
}
