#include "examlock_native.h"
#include "sha256.h"

#include <codecvt>
#include <iomanip>
#include <locale>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

namespace examlock_native {
namespace {
constexpr const char* kClipboardEmptyFingerprint = "clipboard_empty";
constexpr const char* kClipboardEmptySemanticSignature = "clipboard_empty";
constexpr const char* kClipboardSignatureSeparator = " || ";
constexpr const char* kDiagnosticEllipsis = "\xC3\xA2\xE2\x82\xAC\xC2\xA6";
constexpr int kClipboardModeDiagnosticFull = 1;

void AppendSignatureSeparatorIfNeeded(std::string* builder) {
    if (!builder->empty()) {
        builder->append(kClipboardSignatureSeparator);
    }
}

std::string BuildLowercaseSha256Hex(const std::string& value) {
    Sha256 sha256;
    sha256.Update(reinterpret_cast<const uint8_t*>(value.data()), value.size());
    const auto digest = sha256.FinalBytes();

    std::ostringstream stream;
    stream << std::nouppercase << std::hex << std::setfill('0');
    for (const auto byte : digest) {
        stream << std::setw(2) << static_cast<unsigned int>(byte);
    }
    return stream.str();
}

#if defined(__clang__)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif
std::u16string Utf8ToUtf16(const std::string& value) {
    try {
        std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
        return converter.from_bytes(value);
    } catch (...) {
        return std::u16string();
    }
}

std::string Utf16ToUtf8(const std::u16string& value) {
    try {
        std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
        return converter.to_bytes(value);
    } catch (...) {
        return std::string();
    }
}
#if defined(__clang__)
#pragma clang diagnostic pop
#endif

std::string ClipForClipboardDiagnostics(const std::string& value, size_t max_length = 320U) {
    if (value.empty()) {
        return value;
    }

    const std::u16string utf16 = Utf8ToUtf16(value);
    if (!utf16.empty()) {
        if (utf16.size() <= max_length) {
            return value;
        }
        return Utf16ToUtf8(utf16.substr(0, max_length - 1U)) + kDiagnosticEllipsis;
    }

    if (value.size() <= max_length) {
        return value;
    }
    return value.substr(0, max_length - 1U) + kDiagnosticEllipsis;
}

void AppendRawItemSignature(std::string* builder, int index, const ClipboardNormalizedItemCoreInput& item) {
    builder->append("item");
    builder->append(std::to_string(index));
    builder->push_back('=');
    builder->append(item.raw_text);
    builder->push_back('|');
    builder->append(item.raw_html);
    builder->push_back('|');
    builder->append(item.raw_uri);
    builder->push_back('|');
    builder->append(item.raw_intent_action);
    builder->push_back('|');
    builder->append(item.raw_intent_data);
    builder->push_back('|');
    builder->append(item.raw_intent_component);
}

void AppendDecisionItemSignature(std::string* builder, int index, const ClipboardNormalizedItemCoreInput& item) {
    std::string semantic_kind;
    std::string semantic_value;
    if (!item.normalized_text.empty()) {
        semantic_kind = "text";
        semantic_value = item.normalized_text;
    } else if (!item.normalized_html_plain_text.empty()) {
        semantic_kind = "html";
        semantic_value = item.normalized_html_plain_text;
    } else if (!item.normalized_uri.empty()) {
        semantic_kind = "uri";
        semantic_value = item.normalized_uri;
    } else if (
        !item.normalized_intent_action.empty() ||
        !item.normalized_intent_data.empty() ||
        !item.normalized_intent_component.empty()
    ) {
        semantic_kind = "intent";
        semantic_value = item.normalized_intent_action;
        semantic_value.push_back('|');
        semantic_value.append(item.normalized_intent_data);
        semantic_value.push_back('|');
        semantic_value.append(item.normalized_intent_component);
    } else {
        semantic_kind = "empty";
        semantic_value = "-";
    }

    builder->append("item");
    builder->append(std::to_string(index));
    builder->push_back('=');
    builder->append(semantic_kind);
    builder->push_back('|');
    builder->append(semantic_value);
}
}  // namespace

ClipboardSnapshotCoreResult BuildClipboardSnapshotCoreNative(
    int mode,
    const std::vector<ClipboardNormalizedItemCoreInput>& items
) {
    if (items.empty()) {
        return ClipboardSnapshotCoreResult{
            kClipboardEmptyFingerprint,
            kClipboardEmptySemanticSignature,
            std::string(),
            0,
            false,
            false,
            false,
            false,
            true,
        };
    }

    bool has_text = false;
    bool has_html = false;
    bool has_uri = false;
    bool has_intent = false;
    std::string decision_signature;
    std::string raw_signature;
    if (mode == kClipboardModeDiagnosticFull) {
        raw_signature.reserve(items.size() * 32U);
    }

    for (size_t index = 0; index < items.size(); ++index) {
        const ClipboardNormalizedItemCoreInput& item = items[index];
        has_text = has_text || item.has_text;
        has_html = has_html || item.has_html;
        has_uri = has_uri || item.has_uri;
        has_intent = has_intent || item.has_intent;

        AppendSignatureSeparatorIfNeeded(&decision_signature);
        AppendDecisionItemSignature(&decision_signature, static_cast<int>(index), item);

        if (mode == kClipboardModeDiagnosticFull) {
            AppendSignatureSeparatorIfNeeded(&raw_signature);
            AppendRawItemSignature(&raw_signature, static_cast<int>(index), item);
        }
    }

    const bool is_empty = !has_text && !has_html && !has_uri && !has_intent;
    return ClipboardSnapshotCoreResult{
        is_empty ? kClipboardEmptyFingerprint : BuildLowercaseSha256Hex(decision_signature),
        is_empty ? kClipboardEmptySemanticSignature : ClipForClipboardDiagnostics(decision_signature),
        mode == kClipboardModeDiagnosticFull ? raw_signature : std::string(),
        static_cast<int>(items.size()),
        has_text,
        has_html,
        has_uri,
        has_intent,
        is_empty,
    };
}

}  // namespace examlock_native
