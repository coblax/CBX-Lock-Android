package com.example.coblaxexamlock.config

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockSurface

internal val InstallExamKeyboardScript = """
    (function() {
        if (window.__coblaxExamKeyboardInstalled && window.__coblaxExamKeyboard) {
            window.__coblaxExamKeyboard.notifyFocus();
            return;
        }

        var lastEditableElement = null;

        function activeElement() {
            var element = document.activeElement;
            while (element && element.shadowRoot && element.shadowRoot.activeElement) {
                element = element.shadowRoot.activeElement;
            }
            while (element && (element.tagName || '').toUpperCase() === 'IFRAME') {
                try {
                    var frameDocument = element.contentDocument || (element.contentWindow && element.contentWindow.document);
                    if (!frameDocument || !frameDocument.activeElement || frameDocument.activeElement === element) break;
                    element = frameDocument.activeElement;
                    while (element && element.shadowRoot && element.shadowRoot.activeElement) {
                        element = element.shadowRoot.activeElement;
                    }
                } catch (e) {
                    break;
                }
            }
            return element;
        }

        function supportsTextInput(element) {
            if (!element || element.readOnly || element.disabled) return false;
            if (element.isContentEditable) return true;
            var tagName = (element.tagName || '').toUpperCase();
            if (tagName === 'TEXTAREA') return true;
            if (tagName !== 'INPUT') return false;

            var blockedTypes = [
                'button', 'checkbox', 'color', 'date', 'datetime-local', 'file', 'hidden',
                'image', 'month', 'radio', 'range', 'reset', 'submit', 'time', 'week'
            ];
            var type = (element.type || 'text').toLowerCase();
            return blockedTypes.indexOf(type) === -1;
        }

        function isConnected(element) {
            if (!element) return false;
            if (typeof element.isConnected === 'boolean') return element.isConnected;
            return document.documentElement && document.documentElement.contains(element);
        }

        function rememberEditable(element) {
            if (supportsTextInput(element) && isConnected(element)) {
                lastEditableElement = element;
                return element;
            }
            return null;
        }

        function editableElement() {
            var current = rememberEditable(activeElement());
            if (current) return current;
            if (supportsTextInput(lastEditableElement) && isConnected(lastEditableElement)) {
                return lastEditableElement;
            }
            return null;
        }

        function dispatchInput(element) {
            element.dispatchEvent(new Event('input', { bubbles: true }));
            element.dispatchEvent(new Event('change', { bubbles: true }));
        }

        function notifyFocus() {
            var focused = hasEditableFocus();
            if (window.ExamKeyboardBridge && window.ExamKeyboardBridge.onEditableFocusChanged) {
                window.ExamKeyboardBridge.onEditableFocusChanged(focused);
            }
        }

        function hasEditableFocus() {
            return !!editableElement();
        }

        function insertText(text) {
            var element = editableElement();
            if (!supportsTextInput(element)) return false;
            element.focus();

            if (element.isContentEditable) {
                document.execCommand('insertText', false, text);
                notifyFocus();
                return true;
            }

            var value = element.value || '';
            var start = typeof element.selectionStart === 'number' ? element.selectionStart : value.length;
            var end = typeof element.selectionEnd === 'number' ? element.selectionEnd : value.length;
            element.value = value.slice(0, start) + text + value.slice(end);
            var nextPosition = start + text.length;
            if (typeof element.setSelectionRange === 'function') {
                element.setSelectionRange(nextPosition, nextPosition);
            }
            dispatchInput(element);
            notifyFocus();
            return true;
        }

        function backspace() {
            var element = editableElement();
            if (!supportsTextInput(element)) return false;
            element.focus();

            if (element.isContentEditable) {
                document.execCommand('delete');
                notifyFocus();
                return true;
            }

            var value = element.value || '';
            var start = typeof element.selectionStart === 'number' ? element.selectionStart : value.length;
            var end = typeof element.selectionEnd === 'number' ? element.selectionEnd : value.length;

            if (start === end && start > 0) {
                start -= 1;
            }

            element.value = value.slice(0, start) + value.slice(end);
            if (typeof element.setSelectionRange === 'function') {
                element.setSelectionRange(start, start);
            }
            dispatchInput(element);
            notifyFocus();
            return true;
        }

        function moveInputCaret(direction) {
            var element = editableElement();
            if (!supportsTextInput(element) || element.isContentEditable) return false;
            element.focus();

            var value = element.value || '';
            var start = typeof element.selectionStart === 'number' ? element.selectionStart : value.length;
            var end = typeof element.selectionEnd === 'number' ? element.selectionEnd : value.length;
            var nextPosition;

            if (direction === 'left') {
                nextPosition = start === end ? Math.max(0, start - 1) : Math.max(0, start);
            } else {
                nextPosition = start === end ? Math.min(value.length, end + 1) : Math.min(value.length, end);
            }

            if (typeof element.setSelectionRange === 'function') {
                element.setSelectionRange(nextPosition, nextPosition);
            }
            dispatchArrowKey(element, direction === 'left' ? 'ArrowLeft' : 'ArrowRight');
            notifyFocus();
            return true;
        }

        function dispatchArrowKey(element, key) {
            try {
                dispatchKeyboardPair(element, key);
            } catch (e) {}
        }

        function createArrowKeyboardEvent(type, key) {
            var keyCode = key === 'ArrowLeft' ? 37 : 39;
            var event;
            try {
                event = new KeyboardEvent(type, {
                    key: key,
                    code: key,
                    bubbles: true,
                    cancelable: true,
                    composed: true,
                    repeat: false,
                    altKey: false,
                    ctrlKey: false,
                    metaKey: false,
                    shiftKey: false,
                    keyCode: keyCode,
                    which: keyCode
                });
            } catch (e) {
                event = document.createEvent('Event');
                event.initEvent(type, true, true);
            }
            try {
                Object.defineProperty(event, 'key', { get: function() { return key; } });
                Object.defineProperty(event, 'code', { get: function() { return key; } });
                Object.defineProperty(event, 'keyCode', { get: function() { return keyCode; } });
                Object.defineProperty(event, 'which', { get: function() { return keyCode; } });
                Object.defineProperty(event, 'repeat', { get: function() { return false; } });
            } catch (e) {}
            return event;
        }

        function dispatchKeyboardPair(target, key) {
            if (!target || !target.dispatchEvent) return false;
            var down = createArrowKeyboardEvent('keydown', key);
            var cancelled = false;
            try {
                cancelled = target.dispatchEvent(down) === false || down.defaultPrevented;
            } catch (e) {
                return false;
            }
            try {
                target.dispatchEvent(createArrowKeyboardEvent('keyup', key));
            } catch (e) {}
            return cancelled;
        }

        function pushUniqueTarget(targets, target) {
            if (!target) return;
            for (var i = 0; i < targets.length; i += 1) {
                if (targets[i] === target) return;
            }
            targets.push(target);
        }

        function dispatchArrowNavigation(direction) {
            var key = direction === 'left' ? 'ArrowLeft' : 'ArrowRight';
            var element = activeElement();
            var ownerDocument = element && element.ownerDocument ? element.ownerDocument : document;
            var targets = [];

            // The exam web app listens globally on document/window and routes
            // arrows through its own guard + goToQuestion() pipeline.
            pushUniqueTarget(targets, ownerDocument);
            pushUniqueTarget(targets, ownerDocument && ownerDocument.defaultView);
            pushUniqueTarget(targets, document);
            pushUniqueTarget(targets, window);

            for (var i = 0; i < targets.length; i += 1) {
                if (dispatchKeyboardPair(targets[i], key)) {
                    notifyFocus();
                    return true;
                }
            }
            return false;
        }

        function moveContentEditableCaret(direction) {
            var element = editableElement();
            if (!supportsTextInput(element) || !element.isContentEditable) return false;
            element.focus();
            dispatchArrowKey(element, direction === 'left' ? 'ArrowLeft' : 'ArrowRight');

            var selection = window.getSelection ? window.getSelection() : null;
            if (!selection || selection.rangeCount === 0) {
                notifyFocus();
                return true;
            }

            try {
                if (typeof selection.modify === 'function') {
                    selection.modify('move', direction === 'left' ? 'backward' : 'forward', 'character');
                    notifyFocus();
                    return true;
                }
            } catch (e) {}

            try {
                var range = selection.getRangeAt(0).cloneRange();
                if (!range.collapsed) {
                    range.collapse(direction === 'left');
                } else {
                    var container = range.startContainer;
                    var offset = range.startOffset;
                    if (container && container.nodeType === Node.TEXT_NODE) {
                        var textLength = container.textContent ? container.textContent.length : 0;
                        var nextOffset = direction === 'left'
                            ? Math.max(0, offset - 1)
                            : Math.min(textLength, offset + 1);
                        range.setStart(container, nextOffset);
                        range.collapse(true);
                    } else {
                        range.collapse(direction === 'left');
                    }
                }
                selection.removeAllRanges();
                selection.addRange(range);
            } catch (e) {}

            notifyFocus();
            return true;
        }

        function moveCaret(direction) {
            if (!hasEditableFocus()) return false;
            var element = editableElement();
            if (element && element.isContentEditable) {
                return moveContentEditableCaret(direction);
            }
            return moveInputCaret(direction);
        }

        function arrowLeft() {
            return dispatchArrowNavigation('left') || moveCaret('left');
        }

        function arrowRight() {
            return dispatchArrowNavigation('right') || moveCaret('right');
        }

        function enter() {
            var element = editableElement();
            if (!supportsTextInput(element)) return false;
            var tagName = (element.tagName || '').toUpperCase();

            if (element.isContentEditable || tagName === 'TEXTAREA') {
                return insertText('\n');
            }

            if (element.form) {
                if (element.form.requestSubmit) {
                    element.form.requestSubmit();
                } else {
                    element.form.submit();
                }
                return true;
            }

            return insertText('\n');
        }

        window.__coblaxExamKeyboard = {
            insertText: insertText,
            backspace: backspace,
            arrowLeft: arrowLeft,
            arrowRight: arrowRight,
            enter: enter,
            hasEditableFocus: hasEditableFocus,
            notifyFocus: notifyFocus
        };

        ['focusin', 'focusout', 'click'].forEach(function(eventName) {
            document.addEventListener(eventName, function() {
                rememberEditable(activeElement());
                setTimeout(notifyFocus, 0);
            }, true);
        });

        notifyFocus();
        window.__coblaxExamKeyboardInstalled = true;
    })();
""".trimIndent()

internal val InstallExamSideArrowControlsScript = """
    (function() {
        var rootId = '__coblax_exam_side_arrows__';
        if (!document.body || !window.__coblaxExamKeyboard) {
            return false;
        }

        function important(element, key, value) {
            element.style.setProperty(key, value, 'important');
        }

        function makeButton(label, side, action) {
            var button = document.createElement('button');
            var lastInvokeAt = 0;
            button.type = 'button';
            button.setAttribute('aria-label', action === 'left' ? 'Move cursor left' : 'Move cursor right');
            button.setAttribute('tabindex', '-1');
            button.textContent = label;
            important(button, 'position', 'fixed');
            important(button, 'top', '50%');
            important(button, side, '8px');
            important(button, 'transform', 'translateY(-50%)');
            important(button, 'width', '44px');
            important(button, 'height', '74px');
            important(button, 'border-radius', '22px');
            important(button, 'border', '1px solid rgba(31, 91, 176, 0.34)');
            important(button, 'background', 'rgba(255, 255, 255, 0.95)');
            important(button, 'color', '#123B72');
            important(button, 'box-shadow', '0 10px 28px rgba(0, 0, 0, 0.22)');
            important(button, 'font-family', 'sans-serif');
            important(button, 'font-size', '34px');
            important(button, 'font-weight', '800');
            important(button, 'line-height', '70px');
            important(button, 'text-align', 'center');
            important(button, 'padding', '0');
            important(button, 'margin', '0');
            important(button, 'z-index', '2147483647');
            important(button, 'pointer-events', 'auto');
            important(button, 'user-select', 'none');
            important(button, '-webkit-user-select', 'none');
            important(button, '-webkit-tap-highlight-color', 'transparent');
            important(button, 'touch-action', 'manipulation');

            function invoke(event) {
                var now = Date.now();
                if (now - lastInvokeAt < 220) {
                    if (event) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    return false;
                }
                lastInvokeAt = now;
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (event.stopImmediatePropagation) event.stopImmediatePropagation();
                }
                try {
                    if (!window.__coblaxExamKeyboard) return false;
                    return action === 'left'
                        ? !!window.__coblaxExamKeyboard.arrowLeft()
                        : !!window.__coblaxExamKeyboard.arrowRight();
                } catch (e) {
                    return false;
                }
            }

            if (window.PointerEvent) {
                button.addEventListener('pointerdown', invoke, { capture: true, passive: false });
            } else {
                button.addEventListener('touchstart', invoke, { capture: true, passive: false });
                button.addEventListener('mousedown', invoke, { capture: true, passive: false });
            }
            button.addEventListener('click', invoke, true);
            return button;
        }

        function fullscreenHost() {
            return document.fullscreenElement ||
                document.webkitFullscreenElement ||
                document.mozFullScreenElement ||
                document.msFullscreenElement ||
                document.body;
        }

        function installControls() {
            var oldRoot = document.getElementById(rootId);
            if (oldRoot) {
                oldRoot.remove();
            }
            var root = document.createElement('div');
            root.id = rootId;
            important(root, 'position', 'fixed');
            important(root, 'left', '0');
            important(root, 'right', '0');
            important(root, 'top', '0');
            important(root, 'bottom', '0');
            important(root, 'z-index', '2147483647');
            important(root, 'pointer-events', 'none');
            important(root, 'user-select', 'none');
            important(root, '-webkit-user-select', 'none');
            root.appendChild(makeButton('\u2039', 'left', 'left'));
            root.appendChild(makeButton('\u203A', 'right', 'right'));
            fullscreenHost().appendChild(root);
        }

        installControls();
        if (!window.__coblaxExamSideArrowFullscreenListenerInstalled) {
            window.__coblaxExamSideArrowFullscreenListenerInstalled = true;
            ['fullscreenchange', 'webkitfullscreenchange', 'mozfullscreenchange', 'MSFullscreenChange'].forEach(function(eventName) {
                document.addEventListener(eventName, function() {
                    setTimeout(function() {
                        if (window.__coblaxExamKeyboard) installControls();
                    }, 0);
                }, true);
            });
        }
        return true;
    })();
""".trimIndent()

internal val RemoveExamSideArrowControlsScript = """
    (function() {
        var root = document.getElementById('__coblax_exam_side_arrows__');
        if (root) root.remove();
        return true;
    })();
""".trimIndent()

internal val ExamFullscreenRequestHookScript = """
    (function() {
        if (window.__coblaxFullscreenHookInstalled) return;
        window.__coblaxFullscreenHookInstalled = true;

        function isFullscreen() {
            return document.fullscreenElement ||
                document.webkitFullscreenElement ||
                document.mozFullScreenElement ||
                document.msFullscreenElement;
        }

        function requestFullscreen() {
            var element = document.documentElement || document.body;
            if (!element) return;
            var request =
                element.requestFullscreen ||
                element.webkitRequestFullscreen ||
                element.mozRequestFullScreen ||
                element.msRequestFullscreen;
            if (!request) return;
            try { request.call(element); } catch (e) {}
        }

        function attemptFullscreen() {
            if (isFullscreen()) return;
            requestFullscreen();
            if (!isFullscreen()) {
                try {
                    if (
                        window.CBTNativeFullscreenBridge &&
                        typeof window.CBTNativeFullscreenBridge.requestNativeFullscreen === 'function'
                    ) {
                        window.CBTNativeFullscreenBridge.requestNativeFullscreen();
                    }
                } catch (e) {}
            }
        }

        document.addEventListener('click', function onFirstClick() {
            document.removeEventListener('click', onFirstClick, true);
            attemptFullscreen();
        }, true);

        attemptFullscreen();
    })();
""".trimIndent()

internal val ExamNativeFullscreenBridgeInstallScript = """
    (function() {
        if (window.__coblaxNativeFullscreenBridgeInstalled) return;
        window.__coblaxNativeFullscreenBridgeInstalled = true;

        function readNativeActive() {
            try {
                if (
                    window.CBTNativeFullscreenHostBridge &&
                    typeof window.CBTNativeFullscreenHostBridge.isActive === 'function'
                ) {
                    return !!window.CBTNativeFullscreenHostBridge.isActive();
                }
            } catch (e) {}
            return !!window.__CBT_NATIVE_FULLSCREEN_ACTIVE__;
        }

        function emitNativeFullscreen(active) {
            var normalized = !!active;
            window.__CBT_NATIVE_FULLSCREEN_ACTIVE__ = normalized;
            if (window.__coblaxLastNativeFullscreenActive === normalized) return normalized;
            window.__coblaxLastNativeFullscreenActive = normalized;
            var detail = { active: normalized };
            try {
                window.dispatchEvent(new CustomEvent('cbt-native-fullscreen-change', { detail: detail }));
            } catch (e) {}
            try {
                window.dispatchEvent(new CustomEvent('cbt:native-fullscreen-change', { detail: detail }));
            } catch (e) {}
            return normalized;
        }

        var exposedBridge = window.CBTNativeFullscreenBridge || {};
        exposedBridge.isActive = function() {
            return readNativeActive();
        };
        exposedBridge.requestNativeFullscreen = function() {
            try {
                if (
                    window.CBTNativeFullscreenHostBridge &&
                    typeof window.CBTNativeFullscreenHostBridge.requestNativeFullscreen === 'function'
                ) {
                    return !!window.CBTNativeFullscreenHostBridge.requestNativeFullscreen();
                }
            } catch (e) {}
            return false;
        };
        try {
            Object.defineProperty(exposedBridge, 'active', {
                configurable: true,
                enumerable: true,
                get: function() {
                    return readNativeActive();
                }
            });
        } catch (e) {
            exposedBridge.active = readNativeActive();
        }
        window.CBTNativeFullscreenBridge = exposedBridge;
        window.__CBT_SET_NATIVE_FULLSCREEN_ACTIVE__ = emitNativeFullscreen;
        emitNativeFullscreen(readNativeActive());
    })();
""".trimIndent()

internal const val ExamKeyboardBackspaceScript =
    "window.__coblaxExamKeyboard && window.__coblaxExamKeyboard.backspace();"

internal const val ExamKeyboardEnterScript =
    "window.__coblaxExamKeyboard && window.__coblaxExamKeyboard.enter();"

internal val ExamKeyboardArrowLeftScript =
    """
        (function() {
            if (window.__coblaxExamKeyboard) {
                return window.__coblaxExamKeyboard.arrowLeft();
            }
            return false;
        })();
    """.trimIndent()

internal val ExamKeyboardArrowRightScript =
    """
        (function() {
            if (window.__coblaxExamKeyboard) {
                return window.__coblaxExamKeyboard.arrowRight();
            }
            return false;
        })();
    """.trimIndent()

internal val PickerDialogColorScheme = darkColorScheme(
    primary = LockBlue,
    onPrimary = LockOnDark,
    background = LockSurface,
    onBackground = LockOnDark,
    surface = LockSurface,
    onSurface = LockOnDark,
    surfaceVariant = Color(0xFF2B3138),
    onSurfaceVariant = Color(0xFFD4DCE5),
    outline = Color(0xFF4D5C6C)
)
