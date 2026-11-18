package com.templesoftware.frenchverbs.domain.verbs

/** Original, curated verb-specific valencies. No arbitrary object substitution. */
object BaselineSentenceBank {
    val complements: Map<String,List<String>> = mapOf(
        "être" to listOf("ici","au parc","en classe"),
        "avoir" to listOf("un livre","des billes","un vélo"),
        "aller" to listOf("au parc","en ville","à la mer"),
        "faire" to listOf("un dessin","un gâteau","des crêpes"),
        "dire" to listOf("bonjour","merci","un mot"),
        "venir" to listOf("ici","au parc","en classe"),
        "prendre" to listOf("le bus","un livre","des notes"),
        "pouvoir" to listOf("lire","venir","aider"),
        "voir" to listOf("la mer","un chat","des oiseaux"),
        "vouloir" to listOf("lire","jouer","venir"),
        "parler" to listOf("de ce jeu","à un ami","du voyage"),
        "jouer" to listOf("au ballon","aux cartes","dehors"),
        "chanter" to listOf("un air","une chanson","en chœur"),
        "regarder" to listOf("un film","la mer","les oiseaux"),
        "trouver" to listOf("une clé","un livre","des pièces"),
        "manger" to listOf("du pain","une pomme","des pâtes"),
        "nager" to listOf("ici","en mer","à la piscine"),
        "ranger" to listOf("un livre","la salle","des jouets"),
        "partager" to listOf("un pain","un gâteau","des fruits"),
        "changer" to listOf("de place","de jeu","de livre"),
        "commencer" to listOf("un jeu","un dessin","un livre","à lire","ici"),
        "lancer" to listOf("la balle","un dé","des dés"),
        "avancer" to listOf("sans bruit","à petits pas","sur le pont","à pied","ici"),
        "placer" to listOf("un pion","des pions","une chaise"),
        "tracer" to listOf("un trait","un rond","des lignes"),
        "nettoyer" to listOf("la table","un bol","des verres"),
        "essuyer" to listOf("un bol","la table","des verres"),
        "appuyer" to listOf("sur la touche","sur le bouton","sur la sonnette","ici","là"),
        "lever" to listOf("la main","un bras","les bras"),
        "mener" to listOf("la danse","le jeu","une enquête"),
        "amener" to listOf("un ami","le chien","des amis"),
        "emmener" to listOf("un ami","le chien","des amis"),
        "promener" to listOf("le chien","un bébé","les chiens","un chat"),
        "préférer" to listOf("ce jeu","le thé","ces livres"),
        "répéter" to listOf("un mot","un air","des mots"),
        "espérer" to listOf("du soleil","une visite","un cadeau"),
        "compléter" to listOf("un mot","le dessin","une liste","ce mot"),
        "protéger" to listOf("un nid","la nature","des oiseaux"),
        "appeler" to listOf("un ami","le chat","des amis"),
        "rappeler" to listOf("un ami","le médecin","des amis"),
        "jeter" to listOf("un dé","la balle","des dés"),
        "rejeter" to listOf("cette idée","ce projet","ces idées"),
        "envoyer" to listOf("un mot","une carte","des cartes"),
        "renvoyer" to listOf("la balle","un colis","des livres"),
        "finir" to listOf("un jeu","un dessin","un livre"),
        "choisir" to listOf("un jeu","une carte","des livres"),
        "réussir" to listOf("ce jeu","un saut","ces jeux"),
        "grandir" to listOf("vite","en ville","à la ferme"),
        "réfléchir" to listOf("au jeu","à ce choix","à une idée"),
        "remplir" to listOf("un bol","un verre","des verres"),
        "nourrir" to listOf("le chat","un bébé","des oiseaux"),
        "partir" to listOf("en voyage","à pied","en mer"),
        "sortir" to listOf("du bus","du parc","de la salle"),
        "dormir" to listOf("ici","au calme","sous la tente"),
        "sentir" to listOf("la rose","le pain chaud","les fleurs"),
        "servir" to listOf("le thé","un repas","des crêpes"),
        "mentir" to listOf("à un ami","sur son âge","par peur"),
        "mettre" to listOf("un pull","un bonnet","des gants"),
        "permettre" to listOf("ce jeu","une pause","ces jeux"),
        "remettre" to listOf("un pull","un bonnet","des gants"),
        "promettre" to listOf("de venir","un cadeau","de lire"),
        "transmettre" to listOf("un mot","un message","des nouvelles"),
        "connaître" to listOf("ce jeu","la ville","ces chants"),
        "reconnaître" to listOf("un ami","ce lieu","ces voix"),
        "paraître" to listOf("calme","pâle","triste"),
        "apparaître" to listOf("sur la photo","sur scène","à la fenêtre","ici","au loin"),
        "écrire" to listOf("un mot","une lettre","des mots"),
        "décrire" to listOf("un lieu","la ville","des fleurs"),
        "inscrire" to listOf("un nom","la date","des noms"),
        "prescrire" to listOf("du repos","un sirop","des soins"),
        "réécrire" to listOf("un mot","une lettre","des mots"),
        "lire" to listOf("un mot","un livre","des mots"),
        "relire" to listOf("un mot","un livre","des mots"),
        "élire" to listOf("un délégué","une déléguée","des délégués"),
        "réélire" to listOf("un délégué","une déléguée","des délégués"),
        "ouvrir" to listOf("un livre","la porte","des boîtes"),
        "offrir" to listOf("un livre","une fleur","des fleurs"),
        "couvrir" to listOf("un plat","le bol","des plats"),
        "souffrir" to listOf("du froid","de la chaleur","du bruit"),
        "cueillir" to listOf("une fleur","un fruit","des fleurs"),
        "recevoir" to listOf("un livre","une carte","des cartes"),
        "apercevoir" to listOf("un chat","la mer","des oiseaux"),
        "concevoir" to listOf("un jeu","un plan","des jeux"),
        "décevoir" to listOf("un ami","le public","des amis"),
        "percevoir" to listOf("un son","une voix","des bruits"),
        "conduire" to listOf("un bus","le car","un train"),
        "construire" to listOf("un mur","une cabane","des ponts"),
        "produire" to listOf("un son","un dessin","des sons"),
        "traduire" to listOf("un mot","un livre","des mots"),
        "réduire" to listOf("le bruit","la vitesse","les déchets"),
        "peindre" to listOf("un mur","la porte","des fleurs"),
        "craindre" to listOf("le froid","la pluie","les orages"),
        "joindre" to listOf("les mains","deux points","un ami"),
        "atteindre" to listOf("le but","la rive","le sommet"),
        "éteindre" to listOf("le feu","la lampe","les lampes"),
    )
    enum class Gender { MASCULINE, FEMININE }
    data class Agreement(val gender:Gender,val plural:Boolean)
    data class Sentence(val verbId:String,val tense:Int,val person:Int,val agreement:Agreement,
        val pattern:Int,val complement:String,val subject:String,val before:String,val marked:String,val after:String) {
        val text get()=before+marked+after
        val highlightStart get()=before.length
        val highlightEnd get()=before.length+marked.length
    }
    private data class Context(val prefix:String="",val suffix:String="")
    private val contexts=listOf(
        listOf(Context(),Context("Chaque jour, "),Context(suffix=" le soir")),
        listOf(Context("Autrefois, "),Context("Avant, "),Context("À l’époque, ")),
        listOf(Context("Demain, "),Context("Bientôt, "),Context(suffix=" demain")),
        listOf(Context("Hier, "),Context("Ce matin, "),Context(suffix=" hier")),
        listOf(Context("Un jour, "),Context("Puis, "),Context("Alors, ")),
        listOf(Context(suffix=" avant le repas"),Context("À son retour, "),Context("Au réveil, "),Context("Au dîner, "),Context("Au retour, "))
    )
    private val growthContexts=listOf(
        listOf(Context(),Context("Avec le temps, "),Context("Peu à peu, ")),
        contexts[1],listOf(Context("Un jour, "),Context("Avec le temps, "),Context("Bientôt, ")),
        listOf(Context("Cette année, "),Context("L’an dernier, "),Context("En un an, ")),
        contexts[4],listOf(Context("À dix ans, "),Context("À son retour, "),Context("À douze ans, ")))
    fun agreements(person:Int):List<Agreement> = Gender.entries.flatMap { g ->
        (if(person==4)listOf(false,true) else listOf(person==3||person==5)).map{Agreement(g,it)}
    }
    fun candidates(verb:Verb,tense:Int,person:Int):List<Sentence> {
        require(tense in 0..5 && person in 0..5)
        val form=verb.tenses[tense].forms[person]
        return agreements(person).flatMap { agreement ->
            complements.getValue(verb.id).flatMap { complement -> (if(verb.id=="grandir") growthContexts[tense] else contexts[tense]).mapIndexed { pattern,context ->
                val subject=when(person) {
                    2 -> if(agreement.gender==Gender.FEMININE)"elle " else "il "
                    5 -> if(agreement.gender==Gender.FEMININE)"elles " else "ils "
                    else -> form.subject
                }
                // This bank has no preceding COD, pronominal or passive constructions.
                // These five intransitive être verbs have explicitly curated participle forms.
                val agreementEnding=if(tense in listOf(3,5) && verb.auxiliary=="être")
                    participles.getValue(verb.id).getValue(agreement).removePrefix(verb.participle) else form.after
                val actualComplement=if(verb.id=="paraître" && agreement.plural) when(complement) {
                    "calme"->"calmes";"pâle"->"pâles";"triste"->"tristes";else->error("Unreviewed attribute")
                } else complement
                val written=if(context.prefix.isEmpty())subject.replaceFirstChar{it.uppercaseChar()} else subject
                Sentence(verb.id,tense,person,agreement,pattern,actualComplement,subject.trim(),
                    context.prefix+written+form.before,form.marked,agreementEnding+" "+actualComplement+context.suffix+".")
            }}
        }.distinctBy{it.text}
    }
    private fun paradigm(ms:String,fs:String,mp:String,fp:String)=mapOf(
        Agreement(Gender.MASCULINE,false) to ms,Agreement(Gender.FEMININE,false) to fs,
        Agreement(Gender.MASCULINE,true) to mp,Agreement(Gender.FEMININE,true) to fp)
    val participles=mapOf(
        "aller" to paradigm("allé","allée","allés","allées"),
        "venir" to paradigm("venu","venue","venus","venues"),
        "partir" to paradigm("parti","partie","partis","parties"),
        "sortir" to paradigm("sorti","sortie","sortis","sorties"),
        "apparaître" to paradigm("apparu","apparue","apparus","apparues"))
}

/** Explicit-event selection; the caller supplies real rendered widths, not character counts. */
object BaselineSentenceSelection {
    fun choose(candidates:List<BaselineSentenceBank.Sentence>, previous:String?, width:(BaselineSentenceBank.Sentence)->Float,
        available:Float, random:kotlin.random.Random=kotlin.random.Random.Default):Int {
        val fitting=candidates.indices.filter{width(candidates[it])<=available}
        val pool=if(fitting.size>=2)fitting else candidates.indices.sortedBy{width(candidates[it])}.take(3)
        return pool.filter{candidates[it].text!=previous}.ifEmpty{pool}.random(random)
    }
}

