package com.templesoftware.frenchverbs.domain.verbs

/** English-mode choices only. Every profile is bound to the existing verb/complement sense.
 * E = experience (déjà/before), D = dated event (hier/yesterday), R = achieved result (enfin/finally).
 * N = completion by a past noon; A = already completed relative to a past reference.
 * These are example readings, not French-to-English tense conversion rules.
 */
internal object EnglishLearnContexts {
    data class Context(val complement:String,val english:String,val reading:String,
        val prefix:String="",val afterVerb:String="",val compoundAdverb:String="",val suffix:String="")
    private data class Profile(val perfect:Char,val earlier:Char,val narrative:Char)
    // Individually reviewed choices; no sampling across these profiles or the random sentence bank.
    private val rows="""
être E A T
avoir D A T
aller E A T
faire R N U
dire D A T
venir E A U
prendre D N T
pouvoir R A T
voir E A U
vouloir D A T
parler D A T
jouer D A U
chanter E A U
regarder E A T
trouver R N T
manger D N T
nager E A U
ranger R N T
partager D A U
changer D N T
commencer R N T
lancer D A T
avancer D A T
placer D N T
tracer D N T
nettoyer R N T
essuyer D N T
appuyer D A T
lever D A T
mener E A U
amener D N T
emmener D N T
promener D N T
préférer D A T
répéter D A T
espérer D A T
compléter R N T
protéger E A U
appeler D N T
rappeler D N T
jeter D A T
rejeter D A T
envoyer R N T
renvoyer D N T
finir R N T
choisir R N T
réussir R A T
grandir D A T
réfléchir D A T
remplir R N T
nourrir D N T
partir D N T
sortir D A T
dormir E A U
sentir D A T
servir D N T
mentir E A U
mettre D N T
permettre D A T
remettre D N T
promettre D A T
transmettre D N T
connaître D A T
reconnaître R A T
paraître D A T
apparaître E A U
écrire R N T
décrire D A T
inscrire D N T
prescrire D N T
réécrire R N T
lire E A U
relire D A T
élire D A T
réélire D A T
ouvrir R N T
offrir D A U
couvrir D N T
souffrir D A T
cueillir D N T
recevoir R N T
apercevoir E A U
concevoir E A U
décevoir D A T
percevoir D A T
conduire E A U
construire E A U
produire D A T
traduire E A U
réduire R N T
peindre E A U
craindre D A T
joindre D A T
atteindre R N T
éteindre R N T
""".trimIndent()
    private val profiles=rows.lines().associate { val c=it.split(' ');c[0] to Profile(c[1].single(),c[2].single(),c[3].single()) }
    val verbIds get()=profiles.keys

    fun forVerb(verbId:String,tense:Int):Context {
        require(tense in 0..5)
        val pair=BilingualExamples.pairs.getValue(verbId)
        val profile=profiles.getValue(verbId)
        fun c(en:String,reading:String,prefix:String="",after:String="",adverb:String="",suffix:String="")=
            Context(pair.french,en,reading,prefix,after,adverb,suffix)
        val tail=pair.englishComplement
        // A longer life stage is not compressed into yesterday or a single narrative day.
        if(verbId=="grandir")return c(pair.english[tense],when(tense){1->"developing-background";3,4->"completed-life-stage";5->"earlier-life-stage";else->"life-stage"})
        // Connaître's narrative can mark becoming acquainted, unlike its background state.
        // Both members of a comparison use the same person/context in English mode.
        if(verbId=="connaître")return when(tense) {
            0->Context("Léa","I know Léa.","state")
            1->Context("Léa","I already knew Léa.","background-state",afterVerb=" déjà")
            2->Context("Léa","I will know Léa.","future-state")
            3->Context("Léa","I once knew Léa.","former-acquaintance",suffix=" autrefois")
            4->Context("Léa","Then, I got to know Léa.","narrative-onset",prefix="Alors, ")
            else->Context("Léa","I’d known Léa before.","earlier-acquaintance",suffix=" avant")
        }
        // Repeatedly finding an answer is a more useful habitual situation than finding a key.
        if(verbId=="trouver")return when(tense) {
            0->Context("la réponse","I find the answer.","present")
            1->Context("la réponse","I often found the answer.","habit",afterVerb=" souvent")
            2->Context("la réponse","I will find the answer.","future")
            3->Context("la réponse","I’ve finally found the answer.","achieved-result",compoundAdverb="enfin ")
            4->Context("la réponse","That day, I found the answer.","narrative-event",prefix="Ce jour-là, ")
            else->Context("la réponse","By noon, I’d found the answer.","completed-by-past-reference",prefix="À midi, ")
        }
        if(tense==1)return when(verbId) {
            "être"->c("I was still here.","continuing-state",after=" encore")
            "avoir"->c("I was often hungry.","habitual-state",after=" souvent")
            "aller"->c("I often went to town.","habit",after=" souvent")
            "venir"->c("I used to come for dinner.","habit")
            "prendre"->c("I often took the bus.","habit",after=" souvent")
            "préférer"->c("I used to prefer tea.","former-preference",prefix="Autrefois, ")
            "permettre"->c("I often allowed a break.","habit",after=" souvent")
            "élire","réélire"->c(pair.english[1],"habit")
            "pouvoir","vouloir","espérer","reconnaître","paraître","craindre"->c(pair.english[1],"background-state")
            "voir","sentir","apercevoir","percevoir"->c(pair.english[1],"continuing-perception")
            else->c(pair.english[1],"continuing-action")
        }
        if(tense==3)return when {
            verbId=="aller"->c("I’ve been to town before.","experience",adverb="déjà ")
            verbId=="promettre"->c("Yesterday, I promised to come.","dated-promise",prefix="Hier, ")
            profile.perfect=='E'->c("I’ve ${pair.participle} $tail before.","experience",adverb="déjà ")
            profile.perfect=='R'->c("I’ve finally ${pair.participle} $tail.","achieved-result",adverb="enfin ")
            else->c("I ${pair.past} $tail yesterday.","dated-event",suffix=" hier")
        }
        if(tense==4)return when {
            verbId=="avoir"->c("Then, I felt hungry.","narrative-onset",prefix="Alors, ")
            profile.narrative=='U'->c("One day, I ${pair.past} $tail.","narrative-event",prefix="Un jour, ")
            else->c("That day, I ${pair.past} $tail.","narrative-event",prefix="Ce jour-là, ")
        }
        if(tense==5)return when {
            verbId=="aller"->c("I’d been to town before.","earlier-experience",adverb="déjà ")
            verbId=="avoir"->c("I’d been hungry before.","earlier-state",adverb="déjà ")
            verbId in setOf("vouloir","préférer","espérer","paraître","craindre")->
                c("I’d ${pair.participle} $tail before.","earlier-state",adverb="déjà ")
            profile.earlier=='N'->c("By noon, I’d ${pair.participle} $tail.","completed-by-past-reference",prefix="À midi, ")
            else->c("I’d already ${pair.participle} $tail.","earlier-completion",adverb="déjà ")
        }
        return c(pair.english[tense],if(tense==0)"present" else "future")
    }
}
