package com.quicktext.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.quicktext.R

class QuickTextKeyboardService :
    InputMethodService(),
    KeyboardView.OnKeyboardActionListener {

    companion object {
        private const val KEYCODE_SYMBOLS_TOGGLE = -100
        private const val KEYCODE_SWITCH_IME = -101
    }

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var suggestionBar: LinearLayout
    private lateinit var suggestionScroll: HorizontalScrollView
    private lateinit var phrasesDb: PhrasesDatabase

    private var capsOn = false
    private var showingSymbols = false

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this)
            .inflate(R.layout.keyboard_root, null) as LinearLayout

        keyboardView = root.findViewById(R.id.keyboard_view)
        suggestionBar = root.findViewById(R.id.suggestion_bar)
        suggestionScroll = root.findViewById(R.id.suggestion_scroll)

        qwertyKeyboard = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard = Keyboard(this, R.xml.symbols)

        keyboardView.keyboard = qwertyKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        phrasesDb = PhrasesDatabase(this)
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        showingSymbols = false
        capsOn = false
        keyboardView.keyboard = qwertyKeyboard
        updateSuggestions()
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace()
            Keyboard.KEYCODE_SHIFT -> toggleShift()
            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER),
                )
                ic.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER),
                )
            }
            KEYCODE_SYMBOLS_TOGGLE -> toggleSymbols()
            KEYCODE_SWITCH_IME -> switchToNextInputMethod()
            else -> commitCharacter(primaryCode)
        }
        updateSuggestions()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun toggleShift() {
        capsOn = !capsOn
        qwertyKeyboard.isShifted = capsOn
        keyboardView.invalidateAllKeys()
    }

    private fun toggleSymbols() {
        showingSymbols = !showingSymbols
        keyboardView.keyboard = if (showingSymbols) symbolsKeyboard else qwertyKeyboard
        keyboardView.invalidateAllKeys()
    }

    private fun switchToNextInputMethod() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            switchToNextInputMethod(false)
        } catch (_: Throwable) {
            imm.showInputMethodPicker()
        }
    }

    private fun commitCharacter(primaryCode: Int) {
        val ic = currentInputConnection ?: return
        var ch = primaryCode.toChar()
        if (capsOn && ch.isLetter()) {
            ch = ch.uppercaseChar()
            // One-shot shift: reset after committing a letter
            capsOn = false
            qwertyKeyboard.isShifted = false
            keyboardView.invalidateAllKeys()
        }
        ic.commitText(ch.toString(), 1)
    }

    private fun extractLastWord(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
        if (before.isEmpty()) return ""
        // Split by whitespace, last token is the current word
        val tokens = before.split(Regex("\\s+"))
        return tokens.lastOrNull().orEmpty()
    }

    private fun updateSuggestions() {
        val lastWord = extractLastWord()
        suggestionBar.removeAllViews()

        if (lastWord.isBlank()) {
            // Show hint when no word typed
            val hint = TextView(this).apply {
                text = "Gõ phím tắt để xem gợi ý (vd: xc, cb, dc)"
                setTextColor(0xFF888888.toInt())
                textSize = 13f
                setPadding(12, 0, 12, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            suggestionBar.addView(hint)
            return
        }

        val matches = phrasesDb.searchPhrases(lastWord)
        if (matches.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Không có gợi ý cho \"$lastWord\""
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 13f
                setPadding(12, 0, 12, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            suggestionBar.addView(empty)
            return
        }

        val inflater = LayoutInflater.from(this)
        matches.forEach { phrase ->
            val chip = inflater.inflate(
                R.layout.suggestion_item,
                suggestionBar,
                false,
            )
            chip.findViewById<TextView>(R.id.shortcut).text = phrase.shortcut
            chip.findViewById<TextView>(R.id.text).text = phrase.text
            chip.setOnClickListener {
                insertPhrase(phrase)
            }
            suggestionBar.addView(chip)
        }
        suggestionScroll.scrollTo(0, 0)
    }

    private fun insertPhrase(phrase: Phrase) {
        val ic = currentInputConnection ?: return
        val lastWord = extractLastWord()
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }
        ic.commitText(phrase.text + " ", 1)
        updateSuggestions()
    }

    // Required KeyboardView.OnKeyboardActionListener methods
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        if (text != null) ic.commitText(text, 1)
    }
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
