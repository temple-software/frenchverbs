package com.templesoftware.frenchverbs.domain.verbs

/** Sense-specific offline translations: imperfect readings are curated, never guessed from an infinitive. */
internal object BilingualExamples {
    data class Pair(val french:String,val otherFrench:String,val english:List<String>,
        val englishComplement:String,val present:String,val imperfect:String,val base:String,val past:String,val participle:String)
    private val rows="""
être|ici|à la maison|here|am|was|be|was|been
avoir|faim|un vélo|hungry|am|was|be|was|been
aller|en ville|au marché|to town|go|used to go|go|went|gone
faire|un gâteau|un dessin|a cake|make|was making|make|made|made
dire|merci|bonjour|thank you|say|was saying|say|said|said
venir|dîner|aider|for dinner|come|used to come|come|came|come
prendre|le bus|des notes|the bus|take|used to take|take|took|taken
pouvoir|aider|venir|help|can|could|be able to|was able to|been able to
voir|la mer|un cerf|the sea|see|could see|see|saw|seen
vouloir|du lait|jouer dehors|some milk|want|wanted|want|wanted|wanted
parler|à Léa|du voyage|to Léa|talk|was talking|talk|talked|talked
jouer|aux cartes|au tennis|cards|play|was playing|play|played|played
chanter|un air|en chœur|a tune|sing|was singing|sing|sang|sung
regarder|les oiseaux|un dessin animé|the birds|watch|was watching|watch|watched|watched
trouver|une clé|la réponse|a key|find|used to find|find|found|found
manger|du pain|une poire|some bread|eat|was eating|eat|ate|eaten
nager|en mer|dans le lac|in the sea|swim|was swimming|swim|swam|swum
ranger|les jouets|la trousse|the toys|put away|was putting away|put away|put away|put away
partager|une pizza|un secret|a pizza|share|was sharing|share|shared|shared
changer|de place|les draps|seats|change|was changing|change|changed|changed
commencer|un dessin|la course|a drawing|start|was starting|start|started|started
lancer|la balle|un dé|the ball|throw|was throwing|throw|threw|thrown
avancer|à pied|sans bruit|forward|walk|was walking|walk|walked|walked
placer|un pion|une chaise|a counter|place|was placing|place|placed|placed
tracer|un trait|un cercle|a line|draw|was drawing|draw|drew|drawn
nettoyer|un bol|la table|a bowl|clean|was cleaning|clean|cleaned|cleaned
essuyer|la table|les verres|the table|wipe|was wiping|wipe|wiped|wiped
appuyer|sur la touche|sur le bouton|the key|press|was pressing|press|pressed|pressed
lever|la main|les bras|my hand|raise|was raising|raise|raised|raised
mener|une équipe|la danse|a team|lead|was leading|lead|led|led
amener|un ami|du renfort|a friend|bring|was bringing|bring|brought|brought
emmener|le chien|les enfants|the dog along|take|was taking|take|took|taken
promener|le chien|le chiot|the dog|walk|was walking|walk|walked|walked
préférer|le thé|le chocolat|tea|prefer|preferred|prefer|preferred|preferred
répéter|un mot|un refrain|a word|repeat|was repeating|repeat|repeated|repeated
espérer|du soleil|une visite|sunshine|hope for|was hoping for|hope for|hoped for|hoped for
compléter|un mot|le dessin|a word|complete|was completing|complete|completed|completed
protéger|un nid|la forêt|a nest|protect|was protecting|protect|protected|protected
appeler|Léa|le chat|Léa|call|was calling|call|called|called
rappeler|Léa|le médecin|Léa back|call|was calling|call|called|called
jeter|un dé|la balle|a die|throw|was throwing|throw|threw|thrown
rejeter|cette idée|ce projet|this idea|reject|was rejecting|reject|rejected|rejected
envoyer|une carte|un message|a card|send|was sending|send|sent|sent
renvoyer|un colis|la balle|a parcel back|send|was sending|send|sent|sent
finir|un dessin|la soupe|a drawing|finish|was finishing|finish|finished|finished
choisir|un jeu|une poire|a game|choose|was choosing|choose|chose|chosen
réussir|un saut|un tir|a jump|land|was landing|land|landed|landed
grandir|en ville|à la ferme|in town|am growing up|was growing up|grow up|grew up|grown up
réfléchir|à ce choix|au problème|this choice|think about|was thinking about|think about|thought about|thought about
remplir|un bol|le panier|a bowl|fill|was filling|fill|filled|filled
nourrir|le chat|les oiseaux|the cat|feed|was feeding|feed|fed|fed
partir|à pied|en train|on foot|leave|was leaving|leave|left|left
sortir|du bus|de l’école|the bus|get off|was getting off|get off|got off|got off
dormir|ici|sous la tente|here|sleep|was sleeping|sleep|slept|slept
sentir|le café|les fleurs|the coffee|smell|could smell|smell|smelt|smelt
servir|le thé|la soupe|the tea|serve|was serving|serve|served|served
mentir|à Léa|par peur|to Léa|lie|was lying|lie|lied|lied
mettre|un pull|la table|a jumper|put on|was putting on|put on|put on|put on
permettre|une pause|un essai|a break|allow|was allowing|allow|allowed|allowed
remettre|un pull|la clé|a jumper back on|put|was putting|put|put|put
promettre|de venir|de l’aide|to come|promise|was promising|promise|promised|promised
transmettre|un mot|un conseil|a note|pass on|was passing on|pass on|passed on|passed on
connaître|ce jeu|la ville|this game|know|knew|know|knew|known
reconnaître|cet air|un ami|this tune|recognise|recognised|recognise|recognised|recognised
paraître|calme|en forme|calm|seem|seemed|seem|seemed|seemed
apparaître|sur scène|sur la photo|on stage|appear|was appearing|appear|appeared|appeared
écrire|un mot|un poème|a word|write|was writing|write|wrote|written
décrire|un lieu|le paysage|a place|describe|was describing|describe|described|described
inscrire|un nom|la date|a name|write down|was writing down|write down|wrote down|written down
prescrire|du repos|un sirop|rest|prescribe|was prescribing|prescribe|prescribed|prescribed
réécrire|un mot|la phrase|a word|rewrite|was rewriting|rewrite|rewrote|rewritten
lire|un conte|la recette|a story|read|was reading|read|read|read
relire|un mot|le message|a word again|read|was reading|read|read|read
élire|le maire|le capitaine|the mayor|elect|used to elect|elect|elected|elected
réélire|le maire|le capitaine|the mayor|re-elect|used to re-elect|re-elect|re-elected|re-elected
ouvrir|la porte|un bocal|the door|open|was opening|open|opened|opened
offrir|une fleur|de l’aide|a flower|give|was giving|give|gave|given
couvrir|un plat|le lit|a dish|cover|was covering|cover|covered|covered
souffrir|du froid|du dos|from the cold|suffer|was suffering|suffer|suffered|suffered
cueillir|un fruit|du thym|a fruit|pick|was picking|pick|picked|picked
recevoir|une carte|un prix|a card|receive|was receiving|receive|received|received
apercevoir|un chat|un éclair|a cat|catch sight of|could see|catch sight of|caught sight of|caught sight of
concevoir|un jeu|un robot|a game|design|was designing|design|designed|designed
décevoir|un ami|le public|a friend|disappoint|was disappointing|disappoint|disappointed|disappointed
percevoir|un son|une voix|a sound|hear|could hear|hear|heard|heard
conduire|un bus|le groupe|a bus|drive|was driving|drive|drove|driven
construire|un mur|une cabane|a wall|build|was building|build|built|built
produire|un son|un film|a sound|produce|was producing|produce|produced|produced
traduire|un mot|la recette|a word|translate|was translating|translate|translated|translated
réduire|le bruit|le prix|the noise|reduce|was reducing|reduce|reduced|reduced
peindre|un mur|un portrait|a wall|paint|was painting|paint|painted|painted
craindre|la pluie|le gel|the rain|fear|feared|fear|feared|feared
joindre|les mains|deux points|my hands|clasp|was clasping|clasp|clasped|clasped
atteindre|le but|la rive|the goal|reach|was reaching|reach|reached|reached
éteindre|la lampe|le feu|the lamp|switch off|was switching off|switch off|switched off|switched off
""".trimIndent()
    val pairs=rows.lines().associate { row ->
        val c=row.split('|')
        c[0] to Pair(c[1],c[2],listOf(c[4],c[5],"will "+c[6],c[7],c[7],"had "+c[8]).map{"I $it ${c[3]}."},
            c[3],c[4],c[5],c[6],c[7],c[8])
    }
}
