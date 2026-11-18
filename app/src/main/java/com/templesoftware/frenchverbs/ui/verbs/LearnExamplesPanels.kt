package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.domain.verbs.*

internal fun sentenceText(s:SentenceBank.Sentence,green:Color)=buildAnnotatedString {
    append(s.before);withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(s.marked)};append(s.after)
}
@Composable internal fun randomSentenceStyle(landscape:Boolean,compact:Boolean):TextStyle {
    val tight=LocalConfiguration.current.screenHeightDp<850 || LocalDensity.current.fontScale>1.3f
    return MaterialTheme.typography.bodyLarge.copy(
        fontSize=if(landscape)if(compact)17.sp else 18.sp else if(tight)19.sp else 20.sp,
        lineHeight=if(landscape)if(compact)18.sp else 22.sp else if(tight)24.sp else 26.sp,
        platformStyle=PlatformTextStyle(includeFontPadding=false),
        lineHeightStyle=LineHeightStyle(LineHeightStyle.Alignment.Center,LineHeightStyle.Trim.Both))
}
@Composable internal fun ExamplesToggle(active:Boolean,landscape:Boolean,english:Boolean,onClick:()->Unit,modifier:Modifier=Modifier) {
    val direction=if(landscape)if(active)"left" else "right" else if(active)"up" else "down"
    val description=if(english)if(active)"Show conjugation comparison" else "Show examples" else if(active)"Afficher la comparaison" else "Afficher les exemples"
    IconButton(onClick,modifier.size(48.dp).testTag("learn_examples_toggle").semantics{stateDescription=direction}) {
        Surface(Modifier.size(32.dp),shape=CircleShape,color=Color(0xFFE7F1FD),
            border=BorderStroke(1.dp,Color(0xFFBCCFE8))) {
            Box(contentAlignment=Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,description,Modifier.size(20.dp).rotate(
                    when(direction){"right"->180f;"down"->270f;"up"->90f;else->0f}),tint=Color(0xFF234A7C))
            }
        }
    }
}
@Composable internal fun RandomExamplesPanel(sentences:List<SentenceBank.Sentence>,modifier:Modifier,horizontal:Dp) {
    Surface(modifier.testTag("learn_panel_comparison"),shape=RoundedCornerShape(18.dp),
        color=learnPanelColor(MaterialTheme.colorScheme,true),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal=horizontal,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            sentences.forEachIndexed { i,s -> Text(sentenceText(s,learnGreen()),Modifier.testTag("learn_sentence_$i"),style=randomSentenceStyle(false,false)) }
        }
    }
}

/** Pass 17's shared baseline geometry, extended to paired tenses and the six-sentence mode. */
@Composable internal fun LearnLandscapePanels(verb:Verb,tense:Int,comparison:Int,english:Boolean,compact:Boolean,
    dense:Boolean,panelWidth:Dp,horizontal:Dp,modifier:Modifier,active:Boolean,
    sentences:List<SentenceBank.Sentence>,onToggle:()->Unit) {
    val paired=tense!=comparison
    val green=learnGreen()
    val formStyle=MaterialTheme.typography.bodyLarge.copy(fontSize=if(compact)17.sp else 18.sp,
        lineHeight=if(dense)17.sp else if(compact)18.sp else 22.sp,
        platformStyle=PlatformTextStyle(includeFontPadding=false),
        lineHeightStyle=LineHeightStyle(LineHeightStyle.Alignment.Center,LineHeightStyle.Trim.Both))
    val sentenceStyle=formStyle
    val measurer=rememberTextMeasurer()
    val accessible=LocalDensity.current.fontScale>1.3f
    val scaledLandscape=LocalDensity.current.fontScale in 1.01f..1.5f
    val density=LocalDensity.current
    val baseExampleStyle=learnExampleStyle(true,compact,false)
    // Upper examples share one responsive size across all tenses and sentence lengths.
    // Keep the approved paired bottom-group fitting behaviour unchanged.
    val exampleStyle=if(!paired) baseExampleStyle.copy(
        fontSize=if(compact)16.sp else 18.sp,
        lineHeight=if(compact)18.sp else 22.sp
    ) else remember(verb,tense,comparison,english,compact,panelWidth,horizontal,density,baseExampleStyle) {
        val leftExamples=if(paired)LearnExamples.forPanel(verb,tense,true,english) else emptyList()
        val rightExamples=LearnExamples.forPanel(verb,comparison,paired,english)
        val leftWidth=with(density){(panelWidth-horizontal*2-(if(scaledLandscape)0.dp else 48.dp)).toPx()}
        val rightWidth=with(density){(panelWidth-horizontal*2).toPx()}
        val increase=1f
        var size=baseExampleStyle.fontSize.value+increase
        fun style()=baseExampleStyle.copy(fontSize=size.sp,lineHeight=(baseExampleStyle.lineHeight.value+size-baseExampleStyle.fontSize.value).sp)
        while(size>baseExampleStyle.fontSize.value &&
            (leftExamples.any{measurer.measure(exampleText(it,green),style(),softWrap=false).size.width>leftWidth} ||
             rightExamples.any{measurer.measure(exampleText(it,green),style(),softWrap=false).size.width>rightWidth}))size-=.25f
        style()
    }
    val pairedHeights=if(paired)(0..1).map { i ->
        val a=LearnExamples.forPanel(verb,tense,true,english)[i];val b=LearnExamples.forPanel(verb,comparison,true,english)[i]
        val leftWidth=with(density){(panelWidth-horizontal*2-(if(scaledLandscape)0.dp else 48.dp)).roundToPx()}.coerceAtLeast(1)
        val rightWidth=with(density){(panelWidth-horizontal*2).roundToPx()}.coerceAtLeast(1)
        with(density){maxOf(measurer.measure(exampleText(a,green),exampleStyle,constraints=Constraints(maxWidth=leftWidth)).size.height,
            measurer.measure(exampleText(b,green),exampleStyle,constraints=Constraints(maxWidth=rightWidth)).size.height).toDp()}
    } else emptyList()
    fun formText(t:Int,p:Int,highlight:Boolean)=verb.tenses[t].forms[p].let { f -> buildAnnotatedString {
        append(f.subject);append(f.before)
        if(highlight)withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(f.marked)} else append(f.marked)
        append(f.after)
    }}
    @Composable fun Examples(t:Int,tag:String) {
        Column(Modifier.testTag("learn_example_group_$tag")) {
            LearnExamples.forPanel(verb,t,paired,english).forEachIndexed { i,e ->
                Text(exampleText(e,green),Modifier.heightIn(min=pairedHeights.getOrElse(i){0.dp}).testTag("learn_example_${tag}_$i"),style=exampleStyle)
                if(i==0)Spacer(Modifier.height(if(dense)1.dp else 2.dp))
            }
        }
    }
    Layout(modifier=modifier,content={
        for(tag in listOf("current","comparison"))Surface(Modifier.testTag("learn_panel_$tag"),shape=RoundedCornerShape(18.dp),
            color=learnPanelColor(MaterialTheme.colorScheme,tag=="comparison"),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){Box(Modifier)}
        Box { LearnTenseHeading(tense,english,"current",compact,panelWidth-(if(scaledLandscape)0.dp else 48.dp),horizontal) }
        // Unplaced normal-mode slots reserve exactly the same first-panel geometry when toggled.
        Box(if(active)Modifier.clearAndSetSemantics{} else Modifier) {
            if(paired)LearnTenseHeading(comparison,english,"comparison",compact,panelWidth,horizontal)
            else Examples(tense,"comparison")
        }
        for(i in 0..5) {
            Text(formText(tense,i,paired),Modifier.testTag("learn_form_current_$i"),style=formStyle)
            if(active && sentences.size==6)Text(sentenceText(sentences[i],green),Modifier.testTag("learn_sentence_$i"),style=sentenceStyle)
            else Text(formText(comparison,i,true),Modifier.testTag("learn_form_comparison_$i"),style=formStyle)
        }
        Column { if(paired)Examples(tense,"current") }
        Column(if(active)Modifier.clearAndSetSemantics{} else Modifier) {
            if(paired)Examples(comparison,"comparison")
        }
        ExamplesToggle(active,true,english,onToggle)
        HorizontalDivider(Modifier.testTag("learn_divider_current"),thickness=.5.dp)
        HorizontalDivider(Modifier.testTag("learn_divider_comparison"),thickness=.5.dp)
        Column {
            if(paired && verb.auxiliary=="être" && tense in listOf(3,5))Text(stringResource(R.string.agreement),style=MaterialTheme.typography.labelSmall.copy(fontSize=11.sp,lineHeight=12.sp))
        }
        Column(if(active)Modifier.clearAndSetSemantics{} else Modifier) {
            if(verb.auxiliary=="être" && comparison in listOf(3,5))Text(stringResource(R.string.agreement),style=MaterialTheme.typography.labelSmall.copy(fontSize=11.sp,lineHeight=12.sp))
        }
    }) { children,constraints ->
        val gap=10.dp.roundToPx();val width=(constraints.maxWidth-gap)/2
        val inset=horizontal.roundToPx();val rail=48.dp.roundToPx()
        val vertical=(if(dense)2.dp else if(compact)6.dp else 10.dp).roundToPx()
        val innerWidth=(width-2*inset).coerceAtLeast(1)
        val inner=Constraints(maxWidth=innerWidth)
        val left=Constraints(maxWidth=(innerWidth-rail).coerceAtLeast(1))
        val tops=listOf(children[2].measure(if(scaledLandscape)inner else left),children[3].measure(inner))
        val formBounds=left
        val rows=(0..5).flatMap { i -> listOf(children[4+2*i].measure(formBounds),children[5+2*i].measure(inner)) }
        val footers=listOf(children[16].measure(if(scaledLandscape)inner else left),children[17].measure(inner))
        val toggle=children[18].measure(Constraints.fixed(rail,rail))
        val dividers=listOf(children[19].measure(Constraints.fixedWidth(if(scaledLandscape)innerWidth else left.maxWidth)),children[20].measure(Constraints.fixedWidth(innerWidth)))
        val agreements=listOf(children[21].measure(left),children[22].measure(inner))
        // Measure both potential right-row presentations regardless of which one is visible.
        val normal=(0..5).map{measurer.measure(formText(comparison,it,true),formStyle,constraints=inner)}
        val random=(0..5).map{if(sentences.size==6)measurer.measure(sentenceText(sentences[it],green),sentenceStyle,constraints=inner) else normal[it]}
        // At large fonts reserve the whole bank's tallest row, so a refresh cannot move the first panel.
        val accessibleRows=if(accessible)(0..5).map { person ->
            SentenceBank.candidates(verb,tense,person).map{measurer.measure(sentenceText(it,green),sentenceStyle,constraints=inner)}
        } else (0..5).map{listOf(random[it])}
        val ascents=(0..5).map{maxOf(rows[2*it][FirstBaseline],normal[it].firstBaseline.toInt(),accessibleRows[it].maxOf{it.firstBaseline.toInt()})}
        var heights=(0..5).map{ i -> ascents[i]+maxOf(rows[2*i].height-rows[2*i][FirstBaseline],
            normal[i].size.height-normal[i].firstBaseline.toInt(),accessibleRows[i].maxOf{it.size.height-it.firstBaseline.toInt()})}
        // Short phone viewports use the same baseline pitch as a six-line paragraph,
        // avoiding six copies of single-line font leading without shrinking any glyphs.
        if(dense && density.fontScale<=1f) {
            fun paragraph(lines:List<AnnotatedString>,bounds:Constraints)=measurer.measure(
                buildAnnotatedString{lines.forEachIndexed{i,text->if(i>0)append("\n");append(text)}},formStyle,constraints=bounds)
            val paragraphs=listOf(paragraph((0..5).map{formText(tense,it,paired)},left),
                paragraph((0..5).map{formText(comparison,it,true)},inner),
                paragraph((0..5).map{if(sentences.size==6)sentenceText(sentences[it],green) else formText(comparison,it,true)},inner))
            if(paragraphs.all{it.lineCount==6})heights=(0..5).map{i->
                if(i==5)heights[i] else paragraphs.maxOf{ kotlin.math.ceil(it.getLineBaseline(i+1)-it.getLineBaseline(i)).toInt() }
            }
        }
        var rowGap=(if(dense)0.dp else if(compact)2.dp else 4.dp).roundToPx()
        val topGap=(if(dense)1.dp else 3.dp).roundToPx()
        val edge=(if(dense)1.dp else 4.dp).roundToPx()
        val dividerHeight=dividers.maxOf{it.height}
        val agreementHeight=agreements.maxOf{it.height}
        val topMinimum=if(paired)tops.maxOf{it.height}+topGap else
            maxOf(tops[0].height+topGap,tops[1].height+2*edge+dividerHeight+topGap)
        val bottomMinimum=if(paired)maxOf(agreementHeight+edge+dividerHeight+footers.maxOf{it.height}+2*edge,0) else agreementHeight
        // Spend optional row gaps only after required content fits the viewport.
        val gapBudget=(constraints.minHeight-topMinimum-bottomMinimum-2*vertical-heights.sum()).coerceAtLeast(0)
        rowGap=minOf(rowGap,gapBudget/5)
        val rowHeight=heights.sum()+5*rowGap
        val height=maxOf(constraints.minHeight,topMinimum+rowHeight+bottomMinimum+2*vertical,rail+2*vertical)
        val spare=height-2*vertical-topMinimum-rowHeight-bottomMinimum
        val rowsStart=vertical+topMinimum+spare/2
        val rowsEnd=rowsStart+rowHeight
        val backgrounds=children.take(2).map{it.measure(Constraints.fixed(width,height))}
        layout(constraints.maxWidth,height) {
            backgrounds.forEachIndexed{col,p->p.placeRelative(col*(width+gap),0)}
            tops[0].placeRelative(inset,vertical+spare/2)
            if(!active) {
                if(paired)tops[1].placeRelative(width+gap+inset,vertical+spare/2)
                else {
                    val dividerY=rowsStart-topGap-dividerHeight
                    // Centre the complete group between the usable top and the divider.
                    tops[1].placeRelative(width+gap+inset,vertical+(dividerY-vertical-tops[1].height)/2)
                    dividers[1].placeRelative(width+gap+inset,dividerY)
                }
            }
            var y=rowsStart
            for(i in 0..5) {
                val a=rows[2*i];val b=rows[2*i+1]
                a.placeRelative(inset,y+ascents[i]-a[FirstBaseline])
                b.placeRelative(width+gap+inset,y+ascents[i]-b[FirstBaseline])
                y+=heights[i]+if(i<5)rowGap else 0
            }
            agreements[0].placeRelative(inset,rowsEnd)
            if(!active)agreements[1].placeRelative(width+gap+inset,rowsEnd)
            if(paired) {
                val dividerY=rowsEnd+agreementHeight+edge
                val regionTop=dividerY+dividerHeight
                val regionBottom=height-vertical
                for(col in 0..1)if(col==0 || !active) {
                    val x=col*(width+gap)+inset
                    dividers[col].placeRelative(x,dividerY)
                    footers[col].placeRelative(x,regionTop+(regionBottom-regionTop-footers[col].height)/2)
                }
            }
            toggle.placeRelative(width-8.dp.roundToPx()-rail,if(scaledLandscape)rowsStart+(rowsEnd-rowsStart-rail)/2 else height-vertical-rail)
        }
    }
}
