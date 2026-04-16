package com.quicktext.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Switch
import android.widget.TextView
import com.quicktext.R

class QuickTextKeyboardService :
    InputMethodService(),
    KeyboardView.OnKeyboardActionListener {

    companion object {
        private const val KEYCODE_SYMBOLS_TOGGLE = -100
        private const val KEYCODE_SWITCH_IME = -101
        private const val KEYCODE_EMOJI_TOGGLE = -102
    }

    private lateinit var keyboardView: QuickTextKeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard

    private lateinit var phraseSuggestionBar: LinearLayout
    private lateinit var phraseSuggestionScroll: HorizontalScrollView
    private lateinit var wordSuggestionBar: LinearLayout
    private lateinit var wordSuggestionScroll: HorizontalScrollView

    private lateinit var emojiPicker: View
    private lateinit var emojiTabs: LinearLayout
    private lateinit var emojiGrid: GridLayout
    private lateinit var inputContainer: FrameLayout
    private lateinit var globeButton: TextView
    private lateinit var settingsButton: TextView

    private lateinit var phrasesDb: PhrasesDatabase
    private lateinit var wordDict: WordDictionary
    private lateinit var settings: KeyboardSettings
    private val telex = TelexProcessor()
    private var settingsPopup: PopupWindow? = null

    private var capsOn = false
    private var showingSymbols = false
    private var showingEmoji = false
    private var currentEmojiCategory = 0
    /** Whether Vietnamese Telex transformation is active. Toggled by long-pressing space. */
    private var telexEnabled = true

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this)
            .inflate(R.layout.keyboard_root, null) as LinearLayout

        keyboardView = root.findViewById(R.id.keyboard_view)
        phraseSuggestionBar = root.findViewById(R.id.phrase_suggestion_bar)
        phraseSuggestionScroll = root.findViewById(R.id.phrase_suggestion_scroll)
        wordSuggestionBar = root.findViewById(R.id.word_suggestion_bar)
        wordSuggestionScroll = root.findViewById(R.id.word_suggestion_scroll)
        inputContainer = root.findViewById(R.id.input_container)

        emojiPicker = root.findViewById(R.id.emoji_picker)
        emojiTabs = emojiPicker.findViewById(R.id.emoji_tabs)
        emojiGrid = emojiPicker.findViewById(R.id.emoji_grid)
        emojiPicker.findViewById<View>(R.id.emoji_back_button)
            .setOnClickListener { hideEmojiPicker() }
        emojiPicker.findViewById<View>(R.id.emoji_backspace_button)
            .setOnClickListener { handleBackspace(); updateSuggestions() }

        qwertyKeyboard = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard = Keyboard(this, R.xml.symbols)

        settings = KeyboardSettings(this)

        keyboardView.keyboard = qwertyKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = settings.keyPreviewEnabled
        keyboardView.userPreviewEnabled = settings.keyPreviewEnabled
        keyboardView.glideEnabled = settings.glideEnabled
        keyboardView.onSwipeComplete = { letters ->
            handleSwipeComplete(letters)
        }

        globeButton = root.findViewById(R.id.globe_button)
        globeButton.setOnClickListener { toggleTelexMode() }
        updateGlobeButtonLabel()

        settingsButton = root.findViewById(R.id.settings_button)
        settingsButton.setOnClickListener { showSettingsPopup() }

        phrasesDb = PhrasesDatabase(this)
        wordDict = WordDictionary(this)
        updateSpaceKeyLabel()
        return root
    }

    @SuppressLint("InflateParams")
    private fun showSettingsPopup() {
        // If popup is already open, dismiss instead.
        settingsPopup?.let {
            if (it.isShowing) {
                it.dismiss()
                settingsPopup = null
                return
            }
        }

        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_settings, null)

        val glideSwitch = popupView.findViewById<Switch>(R.id.switch_glide)
        glideSwitch.isChecked = settings.glideEnabled
        glideSwitch.setOnCheckedChangeListener { _, checked ->
            settings.glideEnabled = checked
            keyboardView.glideEnabled = checked
        }

        val previewSwitch = popupView.findViewById<Switch>(R.id.switch_preview)
        previewSwitch.isChecked = settings.keyPreviewEnabled
        previewSwitch.setOnCheckedChangeListener { _, checked ->
            settings.keyPreviewEnabled = checked
            keyboardView.userPreviewEnabled = checked
        }

        val autoCapsSwitch = popupView.findViewById<Switch>(R.id.switch_autocaps)
        autoCapsSwitch.isChecked = settings.autoCapsEnabled
        autoCapsSwitch.setOnCheckedChangeListener { _, checked ->
            settings.autoCapsEnabled = checked
            if (checked) updateAutoCaps()
        }

        popupView.findViewById<View>(R.id.btn_switch_keyboard).setOnClickListener {
            settingsPopup?.dismiss()
            settingsPopup = null
            switchToNextInputMethod()
        }

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            elevation = 12f
            isOutsideTouchable = true
            isFocusable = true
        }
        // Anchor above the settings button so it appears between suggestion bar and keyboard.
        popup.showAsDropDown(settingsButton, -200, -keyboardView.height)
        settingsPopup = popup
    }

    private fun toggleTelexMode() {
        telexEnabled = !telexEnabled
        // Finalize any in-progress composing word when switching modes.
        if (telex.hasComposing()) {
            telex.reset()
            currentInputConnection?.finishComposingText()
        }
        updateGlobeButtonLabel()
        updateSpaceKeyLabel()
    }

    private fun updateGlobeButtonLabel() {
        globeButton.text = if (telexEnabled) "🌐 VI" else "🌐 EN"
    }

    /**
     * Update the space key's label to reflect the current input language.
     * Applied to both the QWERTY and symbols keyboards so it stays consistent
     * after switching layouts.
     */
    private fun updateSpaceKeyLabel() {
        val label = if (telexEnabled) "Vi" else "En"
        listOf(qwertyKeyboard, symbolsKeyboard).forEach { kb ->
            kb.keys?.forEach { key ->
                if (key.codes?.firstOrNull() == 32) key.label = label
            }
        }
        if (::keyboardView.isInitialized) keyboardView.invalidateAllKeys()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        showingSymbols = false
        keyboardView.keyboard = qwertyKeyboard
        hideEmojiPicker()
        // Reset Telex state whenever we attach to a new input field.
        telex.reset()
        updateAutoCaps()
        updateSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
        )
        // Only re-check when the cursor position actually changed (avoids fighting
        // our own composing-text updates).
        if (newSelStart == newSelEnd && candidatesStart < 0) {
            updateAutoCaps()
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        currentInputConnection?.finishComposingText()
        telex.reset()
        settingsPopup?.dismiss()
        settingsPopup = null
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // Placeholder / spacer keys used to keep rows visually centred - ignore.
        if (primaryCode == 0) return
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
            KEYCODE_EMOJI_TOGGLE -> showEmojiPicker()
            else -> commitCharacter(primaryCode)
        }
        updateSuggestions()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        // If Telex is composing a word, shrink the composing buffer.
        if (telex.hasComposing()) {
            val remaining = telex.backspace()
            if (remaining.isEmpty()) {
                ic.finishComposingText()
            } else {
                ic.setComposingText(remaining, 1)
            }
            return
        }
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        // Cursor may now sit right after a sentence-ending punctuation.
        updateAutoCaps()
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
            capsOn = false
            qwertyKeyboard.isShifted = false
            keyboardView.invalidateAllKeys()
        }

        if (ch.isLetter()) {
            if (telexEnabled) {
                // Run through Telex and update the composing text (underlined).
                val composing = telex.input(ch)
                ic.setComposingText(composing, 1)
            } else {
                // Plain English typing - commit the letter directly.
                ic.commitText(ch.toString(), 1)
            }
        } else {
            // Non-letter: finalize any Telex word, then commit the character normally.
            if (telex.hasComposing()) {
                telex.reset()
                ic.finishComposingText()
            }
            ic.commitText(ch.toString(), 1)
            // After a space / punctuation / newline, re-evaluate auto-caps so the
            // next letter can start capitalised.
            updateAutoCaps()
        }
    }

    /**
     * Ask the input field whether the cursor is at a position where the next
     * letter should be capitalised (start of field, after ". " / "! " / "? ",
     * after newline). Force TYPE_TEXT_FLAG_CAP_SENTENCES so this works even in
     * fields that did not opt in.
     */
    private fun updateAutoCaps() {
        // Respect the user setting; if disabled, never auto-enable shift.
        if (!::settings.isInitialized || !settings.autoCapsEnabled) return
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: return
        val typeWithSentenceCaps =
            editorInfo.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        val shouldCap = ic.getCursorCapsMode(typeWithSentenceCaps) != 0
        if (shouldCap != capsOn) {
            capsOn = shouldCap
            qwertyKeyboard.isShifted = shouldCap
            keyboardView.invalidateAllKeys()
        }
    }

    private fun extractLastWord(): String {
        // Prefer the Telex composing buffer - it's the authoritative view of the
        // word currently being typed.
        if (telex.hasComposing()) return telex.current()
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
        if (before.isEmpty()) return ""
        val tokens = before.split(Regex("\\s+"))
        return tokens.lastOrNull().orEmpty()
    }

    private fun updateSuggestions() {
        val lastWord = extractLastWord()
        phraseSuggestionBar.removeAllViews()
        wordSuggestionBar.removeAllViews()

        if (lastWord.isBlank()) {
            addHintToPhraseBar("Gõ phím tắt để xem gợi ý câu (vd: xc, cb, dc)")
            addHintToWordBar("Gợi ý từ sẽ hiện ở đây khi bạn gõ")
            return
        }

        val phraseMatches = phrasesDb.searchPhrases(lastWord, limit = 8)
        val wordMatches = wordDict.suggest(lastWord, limit = 6)
            .filter { it.lowercase() != lastWord.lowercase() }

        // ---- Top row: phrase suggestions ----
        if (phraseMatches.isEmpty()) {
            addHintToPhraseBar("Không có câu gợi ý cho \"$lastWord\"")
        } else {
            val inflater = LayoutInflater.from(this)
            phraseMatches.forEach { phrase ->
                val chip = inflater.inflate(
                    R.layout.suggestion_item,
                    phraseSuggestionBar,
                    false,
                )
                chip.findViewById<TextView>(R.id.shortcut).text = phrase.shortcut
                chip.findViewById<TextView>(R.id.text).text = phrase.text
                chip.setOnClickListener { insertPhrase(phrase) }
                phraseSuggestionBar.addView(chip)
            }
            phraseSuggestionScroll.scrollTo(0, 0)
        }

        // ---- Bottom row: word suggestions ----
        if (wordMatches.isEmpty()) {
            addHintToWordBar("Không có từ gợi ý")
        } else {
            wordMatches.forEachIndexed { index, word ->
                addWordChip(word)
                if (index < wordMatches.size - 1) addWordDivider()
            }
            wordSuggestionScroll.scrollTo(0, 0)
        }
    }

    private fun addHintToPhraseBar(text: String) {
        phraseSuggestionBar.addView(buildHint(text))
    }

    private fun addHintToWordBar(text: String) {
        wordSuggestionBar.addView(buildHint(text))
    }

    private fun buildHint(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(0xFF999999.toInt())
        textSize = 12f
        setPadding(12, 0, 12, 0)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun addWordChip(word: String) {
        val density = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = word
            textSize = 15f
            setTextColor(0xFF333333.toInt())
            val hPad = (16 * density).toInt()
            setPadding(hPad, 0, hPad, 0)
            gravity = Gravity.CENTER
            minWidth = (64 * density).toInt()
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { insertWord(word) }
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        wordSuggestionBar.addView(tv, lp)
    }

    private fun addWordDivider() {
        val density = resources.displayMetrics.density
        val divider = View(this).apply {
            setBackgroundColor(0xFFD0D4D9.toInt())
        }
        val divLp = LinearLayout.LayoutParams(
            (1 * density).toInt(),
            (18 * density).toInt(),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        wordSuggestionBar.addView(divider, divLp)
    }

    private fun insertPhrase(phrase: Phrase) {
        val ic = currentInputConnection ?: return
        if (telex.hasComposing()) {
            // Replace the underlined composing word with the phrase text.
            telex.reset()
            ic.setComposingText(phrase.text + " ", 1)
            ic.finishComposingText()
        } else {
            val lastWord = extractLastWord()
            if (lastWord.isNotEmpty()) {
                ic.deleteSurroundingText(lastWord.length, 0)
            }
            ic.commitText(phrase.text + " ", 1)
        }
        updateAutoCaps()
        updateSuggestions()
    }

    private fun insertWord(word: String) {
        val ic = currentInputConnection ?: return
        if (telex.hasComposing()) {
            telex.reset()
            ic.setComposingText("$word ", 1)
            ic.finishComposingText()
        } else {
            val lastWord = extractLastWord()
            if (lastWord.isNotEmpty()) {
                ic.deleteSurroundingText(lastWord.length, 0)
            }
            ic.commitText("$word ", 1)
        }
        updateAutoCaps()
        updateSuggestions()
    }

    // ---------- Emoji picker ----------

    private fun showEmojiPicker() {
        if (showingEmoji) return
        // Finalize any in-progress Telex word before switching to emoji picker.
        if (telex.hasComposing()) {
            telex.reset()
            currentInputConnection?.finishComposingText()
        }
        showingEmoji = true
        keyboardView.visibility = View.GONE
        emojiPicker.visibility = View.VISIBLE
        renderEmojiTabs()
        renderEmojiGrid(currentEmojiCategory)
    }

    private fun hideEmojiPicker() {
        showingEmoji = false
        emojiPicker.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
    }

    private fun renderEmojiTabs() {
        emojiTabs.removeAllViews()
        val density = resources.displayMetrics.density
        EmojiData.categories.forEachIndexed { index, category ->
            val tab = TextView(this).apply {
                text = category.label
                textSize = 20f
                val hPad = (16 * density).toInt()
                setPadding(hPad, 0, hPad, 0)
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                alpha = if (index == currentEmojiCategory) 1f else 0.4f
                setOnClickListener {
                    currentEmojiCategory = index
                    renderEmojiTabs()
                    renderEmojiGrid(index)
                }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            emojiTabs.addView(tab, lp)
        }
    }

    private fun renderEmojiGrid(categoryIndex: Int) {
        emojiGrid.removeAllViews()
        val category = EmojiData.categories[categoryIndex]
        val density = resources.displayMetrics.density
        val cellSize = (42 * density).toInt()

        category.emojis.forEach { emoji ->
            val tv = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { commitEmoji(emoji) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = cellSize
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
            }
            emojiGrid.addView(tv, lp)
        }
    }

    private fun commitEmoji(emoji: String) {
        val ic = currentInputConnection ?: return
        if (telex.hasComposing()) {
            telex.reset()
            ic.finishComposingText()
        }
        ic.commitText(emoji, 1)
        updateAutoCaps()
        updateSuggestions()
    }

    /**
     * Called when the user lifts their finger after a glide-typing gesture.
     * The argument is the de-duplicated sequence of letters the finger crossed.
     * We pick the best dictionary match and commit it.
     */
    private fun handleSwipeComplete(letters: String) {
        if (letters.length < 2) return
        val ic = currentInputConnection ?: return

        // Finalize any in-progress Telex composition.
        if (telex.hasComposing()) {
            telex.reset()
            ic.finishComposingText()
        }

        val candidates = wordDict.glideMatches(letters)
        val word = candidates.firstOrNull()
        if (word != null) {
            ic.commitText("$word ", 1)
        } else {
            // Fallback: commit just the first letter so the user isn't stuck
            // with nothing happening on a glide that matched no dictionary word.
            ic.commitText(letters.first().toString(), 1)
        }
        updateAutoCaps()
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
