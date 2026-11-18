package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import org.json.JSONArray
import org.json.JSONObject
import com.templesoftware.frenchverbs.domain.verbs.*

/** Saved choices, never randomised by layout/recomposition. Entry and toggle are the only events. */
internal class LearnExampleState {
    var itemKey by mutableStateOf("")
    var active by mutableStateOf(false)
    var choices by mutableStateOf(emptyList<Int>())
    // Candidates are immutable for a verb/tense. Reuse them during selection and
    // subsequent compositions; choices and non-repetition history remain separate.
    private var bankKey=""
    private var banks=emptyList<List<SentenceBank.Sentence>>()
    private fun candidates(verb:Verb,tense:Int):List<List<SentenceBank.Sentence>> {
        val key="${verb.id}:$tense"
        if(bankKey!=key) {
            banks=(0..5).map{SentenceBank.candidates(verb,tense,it)}
            bankKey=key
        }
        return banks
    }
    private val recent=linkedMapOf<String,String>()
    private val semanticHistory=mutableListOf<SentenceSelection.Recent>()
    fun enter(key:String,verb:Verb,tense:Int,width:(SentenceBank.Sentence)->Float,available:Float) {
        if(itemKey==key)return
        itemKey=key;active=false;refresh(verb,tense,width,available,false)
    }
    fun toggle(verb:Verb,tense:Int,width:(SentenceBank.Sentence)->Float,available:Float) {
        if(!active)refresh(verb,tense,width,available)
        active=!active
    }
    private fun refresh(verb:Verb,tense:Int,width:(SentenceBank.Sentence)->Float,available:Float,record:Boolean=true) {
        choices=(0..5).map { person ->
            val key="${verb.id}:$tense:$person"
            val bank=candidates(verb,tense)[person]
            SentenceSelection.choose(bank,recent[key],width,available,history=semanticHistory).also { if(record) {
                recent[key]=bank[it].text
                semanticHistory.add(SentenceSelection.remembered(bank[it]))
                while(semanticHistory.size>36)semanticHistory.removeAt(0)
            }}
        }
    }
    fun sentences(verb:Verb,tense:Int)=if(choices.size==6)choices.mapIndexed{p,id->candidates(verb,tense)[p][id]} else emptyList()
    fun encode():String=JSONObject().put("version",19).put("key",itemKey).put("active",active).put("choices",JSONArray(choices))
        .put("recent",JSONObject(recent as Map<*,*>))
        .put("semantic",JSONArray(semanticHistory.map{JSONArray(listOf(it.text,it.context,it.complement,it.theme,it.structure,it.introduction))})).toString()
    companion object {
        fun decode(value:String)=LearnExampleState().apply {
            val json=JSONObject(value);if(json.optInt("version")!=19)return@apply
            itemKey=json.getString("key");active=json.getBoolean("active")
            val ids=json.getJSONArray("choices");choices=(0 until ids.length()).map{ids.getInt(it)}
            val history=json.getJSONObject("recent");history.keys().forEach{recent[it]=history.getString(it)}
            json.optJSONArray("semantic")?.let { a -> for(i in 0 until a.length()) {
                val h=a.getJSONArray(i);semanticHistory.add(SentenceSelection.Recent(h.getString(0),h.getString(1),h.getString(2),h.getString(3),h.getString(4),h.getString(5)))
            }}
        }
        val saver=Saver<LearnExampleState,String>(save={it.encode()},restore={decode(it)})
    }
}

