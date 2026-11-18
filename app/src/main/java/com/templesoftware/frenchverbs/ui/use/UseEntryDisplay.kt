package com.templesoftware.frenchverbs.ui.use

/** Presentation only: reinsert dictionary punctuation at letter boundaries.
 * No punctuation beyond the typed prefix is exposed; no second editable string. */
internal fun useDisplayEntry(form:String,letters:String):String {
    val fixed=mutableMapOf<Int,String>();var index=0
    form.forEach { if(it.isLetter())index++ else fixed[index]=fixed[index].orEmpty()+it }
    return buildString { letters.forEachIndexed { i,c -> append(fixed[i].orEmpty());append(c) } }
}
internal const val USE_CURSOR_PHASE_MILLIS=500
