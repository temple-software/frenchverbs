package com.templesoftware.frenchverbs.domain.verbs

/** Original complete situations. The prefix and complement are inseparable, not word pools. */
internal object SentenceSituations {
    // verb | theme:optional bound circumstance>complement
    private val rows = """
être|everyday:ici|nature:au parc|home:à la maison|travel:en route|friends:avec Léa|weather:à l’abri|holidays:en congé|music:au concert
avoir|reading:un livre|hobbies:des billes|food:faim|weather:froid|hobbies:un vélo|school:une règle|music:une flûte|friends:de la chance
aller|nature:au parc|travel:en ville|family:chez mamie|school:à l’école|shopping:au marché|sports:au stade|holidays:au camping|helping:chercher de l’aide
faire|art:un dessin|cooking:un gâteau|music:du piano|sports:du ski|home:le lit|helping:une faveur|school:un calcul|art:À l’atelier, >un portrait
dire|friends:bonjour|helping:merci|family:bonne nuit|friends:la vérité|school:la réponse|helping:où aller|holidays:À Noël, >merci|everyday:ce qui manque
venir|everyday:ici|nature:au parc|family:chez papi|helping:aider|food:dîner|music:au concert|friends:avec Léa|holidays:À Noël, >en famille
prendre|travel:le bus|reading:un livre|food:du riz|cooking:un bol|sports:de l’élan|travel:le métro|art:une photo|school:En classe, >des notes
pouvoir|reading:lire|travel:venir|cooking:cuire du riz|sports:courir|art:dessiner|helping:rendre service|music:chanter|travel:partir tôt
voir|nature:la mer|animals:un chat|animals:un cerf|weather:la neige|family:mamie|art:une œuvre|travel:le port|nature:Au loin, >une île
vouloir|reading:lire|hobbies:jouer|food:du lait|sports:courir|cooking:faire du pain|nature:voir la mer|helping:aider Léa|music:jouer du piano
parler|hobbies:de ce jeu|friends:à un ami|family:à mamie|animals:du chat|cooking:de cuisine|music:du concert|friends:avec Léa|travel:Dans le bus, >tout bas
jouer|sports:au ballon|hobbies:aux cartes|music:du violon|sports:au tennis|hobbies:aux échecs|family:avec papi|art:un rôle|friends:En équipe, >au foot
chanter|music:un air|music:une chanson|music:juste|family:pour bébé|holidays:à la fête|hobbies:en rythme|friends:avec Léa|home:À la maison, >tout bas
regarder|art:un film|nature:la mer|nature:les nuages|weather:la pluie|travel:le plan|sports:le match|animals:un renard|cooking:En cuisine, >le chef
trouver|everyday:une clé|reading:un livre|nature:un galet|school:la solution|travel:le chemin|friends:une idée|shopping:un cadeau|home:Sous le lit, >une chaussette
manger|food:du pain|food:une pomme|food:du melon|cooking:de la soupe|holidays:une glace|family:chez papi|school:à la cantine|friends:Avec Léa, >des noix
nager|sports:ici|sports:en mer|sports:le crawl|holidays:dans le lac|friends:avec Léa|nature:près du bord|hobbies:sous l’eau|sports:En course, >vite
ranger|reading:un livre|home:la salle|music:la flûte|art:les pinceaux|cooking:les bols|school:la trousse|home:les habits|hobbies:Après le jeu, >les pions
partager|food:un pain|food:un gâteau|friends:un secret|hobbies:les cartes|travel:une chambre|food:une pizza|helping:le travail|family:Avec papi, >un repas
changer|everyday:de place|hobbies:de jeu|travel:de train|music:de rythme|home:les draps|art:de couleur|sports:de maillot|school:En classe, >de place
commencer|hobbies:un jeu|art:un dessin|music:à chanter|cooking:la soupe|sports:la course|school:un devoir|hobbies:un puzzle|art:Au musée, >la visite|reading:à lire|hobbies:à jouer|everyday:ici
lancer|sports:la balle|hobbies:un dé|sports:un frisbee|travel:une fusée|helping:un appel|hobbies:un défi|animals:des miettes|sports:Au stade, >le disque
avancer|everyday:sans bruit|everyday:à petits pas|travel:vers la gare|nature:dans la boue|sports:en rythme|home:la chaise|helping:le travail|hobbies:Au jeu, >un pion
placer|hobbies:un pion|hobbies:des pions|art:le vase|school:un accent|music:le micro|food:le plat|nature:un repère|home:À table, >les verres
tracer|art:un trait|art:un rond|art:une étoile|school:un carré|travel:un trajet|hobbies:une spirale|sports:une piste|nature:Sur le sable, >un cœur
nettoyer|home:la table|cooking:un bol|home:le sol|cooking:le four|sports:les skis|animals:la cage|art:un pinceau|helping:Après la fête, >la salle
essuyer|cooking:un bol|home:la table|home:le sol|cooking:une assiette|sports:le banc|art:un pinceau|everyday:les lunettes|weather:Après la pluie, >la selle
appuyer|everyday:sur la touche|everyday:sur le bouton|music:sur la pédale|school:sur le crayon|helping:ce projet|art:sur le pinceau|everyday:sur le frein|home:À la porte, >sur la sonnette
lever|everyday:la main|sports:un bras|home:le store|sports:une jambe|hobbies:le drapeau|food:le verre|art:le rideau|school:En classe, >le doigt
mener|music:la danse|hobbies:le jeu|travel:le groupe|sports:une équipe|school:un débat|helping:un projet|music:le chœur|animals:À la ferme, >les bêtes au pré
amener|friends:un ami|animals:le chien|family:mamie|helping:du renfort|sports:une équipe|music:le pianiste|friends:Léa au bal|animals:À la ferme, >le veau au pré
emmener|friends:un ami|animals:le chien|family:papi|travel:le groupe|sports:l’équipe|friends:Léa au zoo|holidays:les enfants|animals:Dehors, >le chiot
promener|animals:le chien|family:un bébé|family:bébé dehors|animals:le chiot|animals:un âne|animals:le poney|family:les petits|animals:Au jardin, >le chien
préférer|hobbies:ce jeu|food:le thé|food:le riz|music:le jazz|nature:la mer|sports:le ski|weather:la neige|art:Pour peindre, >le bleu
répéter|school:un mot|music:un air|music:un refrain|school:la règle|art:un rôle|sports:le geste|helping:la consigne|friends:Au téléphone, >le nom
espérer|weather:du soleil|friends:une visite|weather:de la pluie|sports:une victoire|family:un retour|friends:une réponse|holidays:partir|school:À l’examen, >réussir
compléter|school:un mot|art:le dessin|school:une phrase|art:un motif|shopping:la commande|hobbies:la collection|travel:le plan|cooking:Pour le repas, >le menu
protéger|animals:un nid|nature:la nature|animals:les abeilles|nature:la forêt|family:bébé|art:la fresque|home:le sol|weather:En hiver, >les plantes
appeler|friends:un ami|animals:le chat|family:mamie|helping:à l’aide|animals:le chien|friends:Léa|everyday:un taxi|home:À table, >les enfants
rappeler|friends:un ami|helping:le médecin|school:la règle|family:papi|helping:la consigne|friends:Léa|travel:le guide|music:Après le concert, >le chanteur
jeter|hobbies:un dé|sports:la balle|home:les déchets|nature:un caillou|hobbies:un sort|sports:le disque|cooking:la peau|food:Après le repas, >les os
rejeter|everyday:cette idée|helping:ce projet|school:une réponse|art:un modèle|travel:ce trajet|friends:une offre|sports:la faute|helping:Au débat, >cet argument
envoyer|friends:un mot|holidays:une carte|family:une photo|school:un devoir|friends:un message|shopping:un colis|music:un son|holidays:En voyage, >une carte
renvoyer|sports:la balle|shopping:un colis|sports:le volant|shopping:un achat|school:la fiche|friends:le message|travel:le billet|hobbies:Au jeu, >la balle
finir|hobbies:un jeu|art:un dessin|food:la soupe|school:un devoir|music:le morceau|art:la fresque|home:le ménage|sports:À la course, >en tête
choisir|hobbies:un jeu|hobbies:une carte|food:une poire|art:le rouge|travel:un trajet|music:un air|shopping:un manteau|hobbies:Au jeu, >un pion
réussir|hobbies:ce jeu|sports:un saut|school:le test|cooking:le pain|art:un portrait|sports:un tir|hobbies:un tour|helping:En équipe, >la mission
grandir|family:vite|travel:en ville|family:près de mamie|nature:à la montagne|travel:au Canada|school:en sagesse|family:avec deux sœurs|everyday:Avec le temps, >en maturité
réfléchir|hobbies:au jeu|everyday:à ce choix|school:au problème|travel:au trajet|friends:à la réponse|shopping:au prix|hobbies:avant de jouer|helping:En équipe, >au projet
remplir|cooking:un bol|food:un verre|cooking:le moule|travel:le sac|school:une fiche|home:la baignoire|shopping:le panier|nature:À la source, >la gourde
nourrir|animals:le chat|family:un bébé|animals:les poules|family:bébé|nature:les canards|hobbies:un rêve|helping:les voisins|animals:À la ferme, >le veau
partir|travel:en voyage|everyday:à pied|travel:en train|school:en classe verte|friends:avec Léa|sports:en course|holidays:au ski|nature:À l’aube, >en forêt
sortir|travel:du bus|nature:du parc|home:de la cuisine|school:de l’école|travel:du métro|friends:avec Léa|holidays:du camping|weather:Après la pluie, >dehors
dormir|everyday:ici|home:au calme|travel:dans le train|home:sur le canapé|holidays:à l’hôtel|family:chez mamie|nature:à la belle étoile|weather:Au frais, >bien
sentir|nature:la rose|food:le pain chaud|food:le café|nature:le vent|cooking:la soupe|weather:le froid|home:le savon|animals:Au zoo, >le foin
servir|food:le thé|food:un repas|food:du riz|cooking:la soupe|family:le goûter|friends:les invités|helping:à manger|holidays:À la fête, >du jus
mentir|friends:à un ami|everyday:sur ce fait|family:à papi|school:sur la note|friends:à Léa|everyday:par honte|hobbies:sur le score|helping:Par peur, >au médecin
mettre|everyday:un pull|weather:un bonnet|home:la table|food:du sel|art:de la couleur|music:un disque|travel:la ceinture|weather:En hiver, >un manteau
permettre|hobbies:ce jeu|everyday:une pause|school:un essai|sports:une revanche|music:un solo|friends:une visite|helping:ce choix|home:À la maison, >ce jeu
remettre|everyday:un pull|weather:un bonnet|home:la clé|school:la copie|art:le cadre|music:le disque|sports:le prix|food:À table, >du sel
promettre|friends:de venir|family:un cadeau|helping:de l’aide|friends:de revenir|family:une visite|school:de travailler|nature:de planter un arbre|music:Après le concert, >de revenir
transmettre|friends:un mot|friends:un message|school:le savoir|music:un air|family:une recette|travel:un plan|helping:un conseil|friends:Au téléphone, >la nouvelle
connaître|hobbies:ce jeu|travel:la ville|music:cet air|animals:ce chien|travel:le chemin|friends:Léa|cooking:la recette|nature:En forêt, >les arbres
reconnaître|friends:un ami|travel:ce lieu|school:une erreur|family:mamie|music:cet air|nature:un chêne|friends:Léa|art:Au musée, >le peintre
paraître|everyday:calme|everyday:pâle|everyday:en forme|sports:à l’aise|friends:sans souci|home:au calme|school:en confiance|art:Sur scène, >calme
apparaître|art:sur la photo|art:sur scène|art:à l’écran|family:sur le seuil|travel:au balcon|nature:au sommet|hobbies:dans le film|music:Au concert, >sur scène|everyday:ici|nature:au loin
écrire|school:un mot|friends:une lettre|friends:à Léa|music:un chant|school:la date|art:un poème|cooking:la recette|holidays:En voyage, >une carte
décrire|travel:un lieu|travel:la ville|animals:un tigre|weather:l’orage|art:un tableau|family:la maison|travel:le trajet|school:À l’oral, >une scène
inscrire|school:un nom|everyday:la date|sports:le score|school:un chiffre|travel:le lieu|music:le titre|friends:une adresse|shopping:Sur la liste, >du lait
prescrire|helping:du repos|helping:un sirop|helping:un bilan|sports:des étirements|food:un régime|everyday:un examen|helping:une crème|helping:Au cabinet, >une radio
réécrire|school:un mot|friends:une lettre|art:un conte|school:la réponse|music:un couplet|cooking:la recette|friends:le message|travel:Sur le plan, >le nom
lire|school:un mot|reading:un livre|reading:un conte|music:la partition|travel:le plan|cooking:la recette|school:la consigne|home:Au lit, >une BD
relire|school:un mot|reading:un livre|reading:un conte|music:le couplet|school:la règle|cooking:la recette|friends:le message|home:Au calme, >le poème
élire|school:un délégué|school:une déléguée|school:le délégué|sports:le capitaine|helping:le maire|hobbies:le président|art:le jury|sports:Au club, >la présidente
réélire|school:un délégué|school:une déléguée|school:le délégué|sports:le capitaine|helping:le maire|hobbies:le président|art:le jury|sports:Au club, >la présidente
ouvrir|reading:un livre|home:la porte|home:la fenêtre|food:un bocal|shopping:le magasin|music:le bal|travel:la valise|holidays:À Noël, >un cadeau
offrir|reading:un livre|nature:une fleur|food:du chocolat|family:un dessin|friends:un sourire|helping:de l’aide|music:un concert|holidays:À Noël, >un vélo
couvrir|cooking:un plat|food:le bol|home:le lit|school:un cahier|food:la tarte|family:bébé|nature:les semis|weather:Avant le gel, >les rosiers
souffrir|weather:du froid|weather:de la chaleur|everyday:du dos|sports:du genou|weather:du gel|family:de la solitude|helping:de la faim|sports:Après l’effort, >des jambes
cueillir|nature:une fleur|food:un fruit|food:une poire|cooking:du thym|nature:du muguet|food:des mûres|cooking:du persil|holidays:Au verger, >des prunes
recevoir|reading:un livre|friends:une carte|family:mamie|friends:un message|school:une note|sports:un prix|shopping:un colis|holidays:À Noël, >un cadeau
apercevoir|animals:un chat|nature:la mer|animals:un daim|nature:une île|weather:un éclair|family:papi|travel:la gare|sports:Au loin, >le stade
concevoir|hobbies:un jeu|art:un plan|art:un motif|hobbies:un robot|travel:un trajet|home:une maison|helping:un outil|school:En équipe, >un projet
décevoir|friends:un ami|art:le public|family:papi|sports:l’équipe|friends:Léa|school:le jury|music:les fans|art:Sur scène, >le public
percevoir|music:un son|friends:une voix|music:une note|animals:un cri|nature:un écho|weather:un grondement|travel:un signal|home:Au calme, >un murmure
conduire|travel:un bus|travel:le car|travel:un taxi|helping:le groupe|sports:une équipe|animals:le troupeau|hobbies:un kart|travel:En ville, >un taxi
construire|home:un mur|hobbies:une cabane|hobbies:un robot|nature:un abri|art:une maquette|travel:un bateau|home:une étagère|holidays:Sur la plage, >un château
produire|music:un son|art:un dessin|music:un rythme|cooking:du pain|art:un film|nature:du miel|school:un texte|music:Au studio, >un disque
traduire|school:un mot|reading:un livre|school:la phrase|friends:le message|cooking:la recette|music:le chant|reading:un conte|travel:Au café, >le menu
réduire|everyday:le bruit|travel:la vitesse|cooking:le feu|shopping:le prix|sports:l’écart|art:le motif|travel:le trajet|home:La nuit, >le bruit
peindre|home:un mur|home:la porte|art:un portrait|nature:la mer|home:un volet|animals:un lion|hobbies:une figurine|holidays:À la plage, >les vagues
craindre|weather:le froid|weather:la pluie|animals:le loup|weather:le gel|sports:la chute|travel:le retard|family:la séparation|nature:En forêt, >le feu
joindre|everyday:les mains|art:deux points|friends:Léa|school:une copie|art:deux traits|home:deux bouts|helping:le médecin|travel:Au dossier, >une photo
atteindre|sports:le but|nature:la rive|sports:la cible|travel:le port|nature:le lac|school:le niveau|home:l’étagère|sports:À la course, >la finale
éteindre|home:le feu|home:la lampe|home:le four|music:la radio|hobbies:l’écran|holidays:les bougies|helping:l’incendie|school:En sortant, >la lumière
""".trimIndent()
    val patterns: Map<String,List<SentenceBank.Pattern>> = rows.lines().associate { row ->
        val cells=row.split('|')
        cells.first() to cells.drop(1).mapIndexed { index,cell ->
            val theme=cell.substringBefore(':');val body=cell.substringAfter(':')
            val prefix=if('>' in body)body.substringBefore('>') else ""
            val tail=body.substringAfter('>')
            val structure=when {
                prefix.isNotEmpty()->"circumstance"
                tail.startsWith("de ") || tail.startsWith("du ")->"source-or-topic"
                tail.startsWith("à ") || tail.startsWith("au ") || tail.startsWith("en ") || tail.startsWith("chez ")->"prepositional"
                tail.startsWith("un ") || tail.startsWith("une ") || tail.startsWith("le ") || tail.startsWith("la ") || tail.startsWith("les ") || tail.startsWith("des ")->"object"
                else->"predicate-or-infinitive"
            }
            SentenceBank.Pattern("${cells.first()}:$index",cells.first(),tail,theme,structure,prefix)
        }
    }
}
