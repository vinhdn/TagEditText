package com.vinhdn.tagedittext

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import java.lang.StringBuilder

class TagEditText : AppCompatEditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var onSearchTag: (String?) -> Unit = {}
    var onTagClick: (TextTag) -> Unit = {}

    private var lastText: StringBuilder = StringBuilder()
    var tags = mutableListOf<TextTag>()
    private var isInTag = false
    private var start = -1
    @ColorInt var tagColor = Color.BLUE
    var tagCharacter = '@'

    init {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(newText: CharSequence, startChange: Int, count: Int, after: Int) {
                var isDeleteSpan = false
                if (lastText.length > newText.length) {
                    for ((index, tag) in tags.withIndex()) {
                        if (startChange <= tag.start && startChange + count >= tag.end) {
                            isDeleteSpan = true
                            tags.removeAt(index)
                            break
                        }
                    }
                    if (!isDeleteSpan && deleteSpan()) {
                        return
                    }
                }
                if (newText.isNotEmpty()) {
                    val lastChar = newText[newText.length - 1]
                    when (lastChar) {
                        tagCharacter -> {
                            isInTag = true
                            onSearchTag("")
                            start = newText.length - 1
                        }
                        ' ' -> {
                            isInTag = false
                            onSearchTag(null)
                            start = -1
                        }
                        else -> {
                            if (isInTag ) {
                                if (start + 1 < newText.length) {
                                    onSearchTag(newText.substring(start + 1))
                                }
                            } else {
                                if (newText.lastIndexOf(tagCharacter) > newText.lastIndexOf(' ')) {
                                    start = newText.lastIndexOf(tagCharacter)
                                    isInTag = true
                                    onSearchTag(newText.substring(start + 1))
                                }
                            }
                        }
                    }
                }
                lastText.clear()
                lastText.append(newText)
            }
        })
        setTextIsSelectable(false)
        movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart == selEnd) {
            super.onSelectionChanged(selStart, selEnd)
            return
        }
        var realStart = selStart
        var realEnd = selEnd
        for (tag in tags) {
            if (realStart <= tag.end && realStart >= tag.start) {
                realStart = tag.start
                break
            }
        }
        for (tag in tags) {
            if (realEnd <= tag.end && realEnd >= tag.start) {
                realEnd = tag.end
                break
            }
        }
        if (realStart != selStart || realEnd != selEnd) {
            post {
                setSelection(realStart, realEnd)
            }
        }
    }

    fun addTag(name: String, id: String) {
        if (start >= 0) {
            var textSpan = text!!
            textSpan = textSpan.delete(start, text!!.length)
            textSpan.append(tagCharacter)
            textSpan.append(name)
            tags.add(TextTag(start, start + name.length + 1, name, id))
            val span: CharacterStyle = ClickableForegroundColorSpan(tagColor) { text, end ->
                post {
                    setSelection(end)
                }
            }
            textSpan.setSpan(span, start, start + name.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            textSpan.append(" ")
            start = -1
            isInTag = false
            text = textSpan
            setSelection(text!!.length)
        }
    }

    private fun deleteSpan(): Boolean {
        val currentCursor = selectionEnd
        for ((index, tag) in tags.withIndex()) {
            if (currentCursor <= tag.end - 1 && currentCursor >= tag.start) {
                tags.removeAt(index)
                var textSpan = text!!
                if (tag.start >= 0 && tag.end - 1 <= textSpan.length) {
                    textSpan = textSpan.delete(tag.start, tag.end - 1)
                }
                text = textSpan
                post {
                    setSelection(tag.start)
                }
                return true
            }
        }
        return false
    }
}

data class TextTag(
    var start: Int,
    var end: Int,
    var text: String,
    var id: String
)