package com.templesoftware.frenchverbs.domain.verbs

data class Form(val subject: String, val before: String, val marked: String, val after: String) {
    val text get() = subject + before + marked + after
    val highlightStart get() = subject.length + before.length
    val highlightEnd get() = highlightStart + marked.length
}
data class Conjugation(val forms: List<Form>, val examples: List<String>)
data class Verb(val id: String, val meaning: String, val group: String, val usefulnessRank: Int,
    val auxiliary: String, val participle: String, val usage: String, val source: String,
    val tenses: List<Conjugation>)

object Choices {
    val tenses = listOf("present", "imperfect", "future", "perfect", "historic", "pluperfect")
    val types = listOf("important", "er", "ir", "irregular")
    val important = listOf("être", "avoir", "aller", "faire", "dire", "venir", "prendre", "pouvoir", "voir", "vouloir")
    val er = listOf("standard", "ger", "cer", "yer", "e_grave", "accent", "double", "special")
    val families = listOf("partir", "mettre", "connaitre", "ecrire", "lire", "ouvrir", "recevoir", "conduire", "peindre")
    fun ids(section: String) = when(section) {
        "tenses" -> tenses; "types" -> types; "important" -> important; "er" -> er; "families" -> families
        else -> emptyList()
    }
}

data class Selection(
    val tenses: Set<String> = setOf("present"),
    val types: Set<String> = setOf("important"),
    val important: Set<String> = Choices.important.take(4).toSet(),
    val er: Set<String> = Choices.er.take(4).toSet(),
    val families: Set<String> = Choices.families.take(3).toSet(),
) {
    fun selected(section: String) = when(section) {
        "tenses" -> tenses; "types" -> types; "important" -> important; "er" -> er; "families" -> families
        else -> emptySet()
    }
    fun visible(section: String) = Choices.ids(section).filter { section != "er" || it != "special" || "future" in tenses }
    fun with(section: String, values: Set<String>): Selection {
        val valid = values.intersect(Choices.ids(section).toSet())
        return when(section) {
            "tenses" -> copy(tenses = valid); "types" -> copy(types = valid); "important" -> copy(important = valid)
            "er" -> copy(er = valid); "families" -> copy(families = valid); else -> this
        }
    }
    fun toggle(section: String, id: String): Selection {
        if (id !in visible(section)) return this
        val old = selected(section)
        return with(section, if (id in old) old - id else old + id)
    }
    fun all(section: String): Boolean = visible(section).all { it in selected(section) }
    fun toggleAll(section: String): Selection = with(section,
        if (all(section)) selected(section) - visible(section).toSet() else selected(section) + visible(section))
    fun canContinue(section: String) = visible(section).any { it in selected(section) }
    fun setupRoutes() = listOf("tenses", "types") +
        listOfNotNull("important".takeIf { "important" in types }, "er".takeIf { "er" in types }, "families".takeIf { "irregular" in types }) + "activities"
    fun next(section: String): String? = setupRoutes().let { routes -> routes.indexOf(section).takeIf { it >= 0 }?.let { routes.getOrNull(it + 1) } }
}

data class LearningItem(val verbId: String, val tense: Int, val example: Int) {
    fun encode() = "$verbId:$tense:$example"
    companion object {
        fun decode(value: String): LearningItem? {
            val parts = value.split(':'); if (parts.size != 3) return null
            val tense = parts[1].toIntOrNull() ?: return null
            val example = parts[2].toIntOrNull() ?: return null
            if (tense !in 0..5 || example !in 0..1 || OfflineDictionary.verbs.none { it.id == parts[0] }) return null
            return LearningItem(parts[0], tense, example)
        }
    }
}
object LearningSequence {
    fun create(selection: Selection, previous: List<LearningItem> = emptyList(), random: kotlin.random.Random = kotlin.random.Random.Default): List<LearningItem> {
        val verbs = OfflineDictionary.verbs
        val chosen = mutableListOf<String>()
        fun choose(group: String) {
            val pool = verbs.filter { it.group == group && it.id !in chosen }
            val fresh = pool.filter { v -> previous.none { it.verbId == v.id } }
            (fresh.ifEmpty { pool }).randomOrNull(random)?.let { chosen.add(it.id) }
        }
        if ("important" in selection.types) chosen += Choices.important.filter { it in selection.important }
        if ("er" in selection.types) selection.visible("er").filter { it in selection.er }.forEach(::choose)
        if ("ir" in selection.types) choose("ir")
        if ("irregular" in selection.types) Choices.families.filter { it in selection.families }.forEach(::choose)
        return chosen.flatMap { verb -> Choices.tenses.mapIndexedNotNull { i,t ->
            if (t in selection.tenses) LearningItem(verb, i, random.nextInt(2)) else null
        } }
    }
}
