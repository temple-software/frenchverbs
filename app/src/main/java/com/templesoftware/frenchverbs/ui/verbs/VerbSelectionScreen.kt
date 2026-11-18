package com.templesoftware.frenchverbs.ui.verbs

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.VerbState
import com.templesoftware.frenchverbs.ui.components.TempleScreenScaffold
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction
import kotlin.math.floor

private val LocalDenseSelection=staticCompositionLocalOf { false }

/** Only the four verb-selection screens. Tenses and activities use their own renderers. */
@Composable
internal fun VerbSelectionScreen(section:String,state:VerbState,language:AppLanguage,
    onToggle:(String?)->Unit,onNext:()->Unit,onBack:()->Unit) {
    val title=when(section){"types"->R.string.types_title;"important"->R.string.important_title;"er"->R.string.er_title;else->R.string.families_title}
    TempleScreenScaffold(stringResource(title),NavigationAction.BACK,onBack,language) {
        if (section == "er") {
            RegularErSelectionLayout(state, onToggle, onNext)
            return@TempleScreenScaffold
        }
        BoxWithConstraints(Modifier.widthIn(max=1000.dp).fillMaxSize().padding(horizontal=16.dp).testTag("selection_viewport")) {
            val width=maxWidth;val height=maxHeight
            val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
            CompositionLocalProvider(LocalDenseSelection provides ((width<500.dp && height<580.dp) || LocalDensity.current.fontScale in 1.01f..1.5f)) {
            SelectionPageLayout(groupTogether=width>=600.dp && height>=480.dp, options={
                Column(Modifier.verticalScroll(rememberScrollState()).testTag("choices"),verticalArrangement=Arrangement.spacedBy(LocalSelectionGap.current)) {
                    if(state.storageError) Text(stringResource(R.string.storage_error),color=MaterialTheme.colorScheme.error)
                    when(section) {
                        "types"->TypeOptions(state.selection,width,landscape,onToggle)
                        "important"->ImportantOptions(state.selection,width,landscape,onToggle)
                        "er"->ErOptions(state.selection,width,landscape,onToggle)
                        else->FamilyOptions(state.selection,width,landscape,onToggle)
                    }
                }
            },actions={
                Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                    PrimaryAction(stringResource(R.string.next),onNext,Modifier.padding(horizontal=56.dp).widthIn(max=420.dp).fillMaxWidth().testTag("next"),state.selection.canContinue(section))
                }
            })
            }
        }
    }
}

@Composable
private fun Grid(ids:List<String>,columns:Int,width:Dp,balancedLast:Boolean=false,spacing:Dp?=null,scrollHorizontal:Boolean=false,independentColumns:Boolean=false,card:@Composable (String)->Unit) {
    if(independentColumns) {
        Row(Modifier.width(width).testTag("option_grid"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            repeat(columns) { column ->
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(spacing?:LocalSelectionGap.current)) {
                    ids.filterIndexed { index,_ -> index%columns==column }.forEach { card(it) }
                }
            }
        }
        return
    }
    Column(Modifier.then(if(scrollHorizontal)Modifier.horizontalScroll(rememberScrollState()) else Modifier).testTag("option_grid"),verticalArrangement=Arrangement.spacedBy(spacing?:LocalSelectionGap.current)) {
        ids.chunked(columns).forEach { row ->
            Row(Modifier.width(width),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(balancedLast && row.size<columns) Spacer(Modifier.weight((columns-row.size)/2f))
                row.forEach { id -> Box(Modifier.weight(1f)){card(id)} }
                if(balancedLast && row.size<columns) Spacer(Modifier.weight((columns-row.size)/2f))
                else repeat(columns-row.size){Spacer(Modifier.weight(1f))}
            }
        }
    }
}

@Composable
private fun TypeOptions(selection:Selection,width:Dp,landscape:Boolean,onToggle:(String?)->Unit) {
    val density=LocalDensity.current;val measurer=rememberTextMeasurer()
    val columns=if(landscape || width>=480.dp*density.fontScale) 2 else 1
    val labels=Choices.types.associateWith { stringResource(when(it){"important"->R.string.important_title;"er"->R.string.er_title;"ir"->R.string.ir_title;else->R.string.families_title}) }
    val secondary=mapOf("important" to "être, avoir, aller, faire, dire","er" to "parler, manger, commencer","ir" to "finir, choisir, réussir","irregular" to "partir, mettre, connaître")
    val cardWidth=(width-8.dp*(columns-1))/columns
    val textWidth=with(density){(cardWidth-66.dp).roundToPx().coerceAtLeast(1)}
    val titleStyle=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold)
    val heights=Choices.types.map { id ->
        measurer.measure(labels.getValue(id),titleStyle,constraints=Constraints(maxWidth=textWidth)).size.height+
            measurer.measure(secondary.getValue(id),MaterialTheme.typography.bodyMedium,constraints=Constraints(maxWidth=textWidth)).size.height
    }
    val minHeight=if(columns>1) maxOf(60.dp,with(density){heights.max().toDp()}+14.dp) else 60.dp
    Grid(Choices.types,columns,width) { id ->
        SelectionCard(id,labels.getValue(id),id in selection.types,{onToggle(id)},minHeight=minHeight,compact=true) {
            Text(secondary.getValue(id),style=MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ImportantOptions(selection:Selection,width:Dp,landscape:Boolean,onToggle:(String?)->Unit) {
    val density=LocalDensity.current;val measurer=rememberTextMeasurer()
    val style=(if(landscape)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium).copy(fontWeight=FontWeight.Bold)
    val columns=if(landscape) 5 else if(width>=260.dp*density.fontScale) 2 else 1
    val minimum=with(density){Choices.important.maxOf { measurer.measure(it,style,softWrap=false).size.width }.toDp()}+50.dp
    val gridWidth=width
    Grid(Choices.important,columns,gridWidth,balancedLast=landscape,scrollHorizontal=gridWidth>width) { id ->
        SelectionCard(id,id,id in selection.important,{onToggle(id)},minHeight=if(landscape)56.dp else 60.dp,compact=landscape,centredLabel=landscape)
    }
}

@Composable
private fun ErOptions(selection:Selection,width:Dp,landscape:Boolean,onToggle:(String?)->Unit) {
    val measurer=rememberTextMeasurer();val density=LocalDensity.current
    val ids=selection.visible("er")
    val examples=ids.associateWith { stringResource(ChoiceResources.example.getValue(it)) }
    val descriptions=ids.associateWith { stringResource(ChoiceResources.description.getValue(it)) }
    val style=MaterialTheme.typography.bodyMedium
    val essentials=ids.map { examples.getValue(it).substringBefore(" → ")+" · "+descriptions.getValue(it) }
    val minCardWidth=with(density){essentials.maxOf { measurer.measure(it,style,softWrap=false).size.width }.toDp()}+70.dp
    val columns=if(landscape)3 else if(!landscape && width>=320.dp && LocalConfiguration.current.screenHeightDp<900)2 else floor((width.value+8)/(minCardWidth.value+8)).toInt().coerceIn(1,3)
    val displayed=if(selection.all("er"))ids else listOf("all")+ids
    Grid(displayed,columns,width,independentColumns=landscape && density.fontScale in 1.01f..1.5f) { id ->
        if(id=="all") SelectionCard(id,stringResource(R.string.all),false,{onToggle(null)},minHeight=if(landscape)54.dp else 60.dp,compact=true,insetText=true)
        else SelectionCard(id,stringResource(ChoiceResources.category.getValue(id)),id in selection.er,{onToggle(id)},compact=landscape,dense=landscape,insetText=true,minHeight=if(landscape)54.dp else 60.dp) {
            MeasuredCategoryLine(id,examples.getValue(id),descriptions.getValue(id))
        }
    }
}

@Composable
private fun MeasuredCategoryLine(id:String,example:String,description:String) {
    val measurer=rememberTextMeasurer();val style=if(LocalConfiguration.current.screenHeightDp<500 || LocalDenseSelection.current)MaterialTheme.typography.bodyMedium.copy(fontSize=12.sp,lineHeight=16.sp) else MaterialTheme.typography.bodyMedium
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("secondary_slot_$id")) {
        val full="$example · $description"
        val compact=example.substringBefore(" → ")+" · "+description
        val showFull=measurer.measure(full,style,softWrap=false,maxLines=1).size.width<=constraints.maxWidth
        Text(if(showFull)full else compact,style=style,modifier=Modifier.fillMaxWidth().testTag("secondary_$id"))
    }
}

@Composable
private fun FamilyOptions(selection:Selection,width:Dp,landscape:Boolean,onToggle:(String?)->Unit) {
    val density=LocalDensity.current;val measurer=rememberTextMeasurer()
    val members=Choices.families.associateWith { stringResource(ChoiceResources.members.getValue(it)) }
    val tablet=width>=600.dp && LocalConfiguration.current.screenHeightDp>=600
    val portraitColumns=when { tablet && density.fontScale<=1.5f->3;width>=320.dp && LocalConfiguration.current.screenHeightDp<900->2;width>=480.dp*density.fontScale->2;else->1 }
    val groups=if(landscape || tablet)Choices.families.chunked(3) else listOf(Choices.families.take(3),Choices.families.drop(3).take(4),Choices.families.drop(7))
    val titles=Choices.families.associateWith { stringResource(ChoiceResources.family.getValue(it)) }
    val short=LocalConfiguration.current.screenHeightDp<500
    val dense=LocalDenseSelection.current
    val padding=if(dense)20.dp else if(short)16.dp else 32.dp
    val tick=if(short)24.dp else 30.dp
    val cellWidth=(width-8.dp*((if(landscape)3 else portraitColumns)-1))/(if(landscape)3 else portraitColumns)
    val textWidth=with(density){(cellWidth-padding-tick-8.dp).roundToPx().coerceAtLeast(1)}
    val heading=if(short)MaterialTheme.typography.titleMedium.copy(fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold)
    val memberStyle=MaterialTheme.typography.bodySmall
    val cardVerticalPadding=if(dense || LocalConfiguration.current.screenHeightDp<=360)4.dp else 12.dp
    fun rowHeight(ids:List<String>)=with(density){ids.maxOf { id ->
        val titleHeight=measurer.measure(titles.getValue(id),heading,constraints=Constraints(maxWidth=textWidth)).size.height
        val memberHeight=measurer.measure(members.getValue(id).substringBefore(", "),memberStyle,constraints=Constraints(maxWidth=textWidth)).size.height
        maxOf((if(short)24.dp else 30.dp).roundToPx(),titleHeight+memberHeight+2.dp.roundToPx())
    }.toDp()}+cardVerticalPadding

    Column(verticalArrangement=Arrangement.spacedBy(if(landscape)LocalSelectionGap.current.coerceAtLeast(4.dp) else (LocalSelectionGap.current-3.dp)/2)) {
        groups.forEachIndexed { index,group ->
            if(index>0 && !landscape) Spacer(Modifier.padding(vertical=1.dp).height(1.dp)) // Preserve the divider's separate pixel rounding, without drawing a line.
            Grid(group,if(landscape)3 else minOf(group.size,portraitColumns),width) { id ->
                SelectionCard(id,stringResource(ChoiceResources.family.getValue(id)),id in selection.families,{onToggle(id)},priority=id in Choices.families.take(3),compact=true,insetText=true,minHeight=maxOf(if(LocalConfiguration.current.screenHeightDp<=360)48.dp else 60.dp,rowHeight(group))) {
                    MeasuredMembers(id,members.getValue(id))
                }
            }
        }
    }
}

@Composable
private fun MeasuredMembers(id:String,members:String) {
    val measurer=rememberTextMeasurer();val style=MaterialTheme.typography.bodySmall
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("secondary_slot_$id")) {
        val parts=members.split(", ")
        val fitting=(parts.size downTo 1).map { parts.take(it).joinToString(", ") }.firstOrNull {
            measurer.measure(it,style,softWrap=false,maxLines=1).size.width<=constraints.maxWidth
        }
        Text(fitting?:parts.first(),style=style,softWrap=true,
            modifier=Modifier.testTag("secondary_$id").fillMaxWidth())
    }
}

@Composable
private fun SelectionCard(id:String,title:String,checked:Boolean,onClick:()->Unit,minHeight:Dp=60.dp,
    compact:Boolean=false,priority:Boolean=false,centredLabel:Boolean=false,dense:Boolean=false,insetText:Boolean=false,secondary:(@Composable ()->Unit)?=null) {
    val short=LocalConfiguration.current.screenHeightDp<500
    val veryShort=LocalConfiguration.current.screenHeightDp<=360
    val dark=MaterialTheme.colorScheme.background.luminance()<0.5f
    val tint=if(priority) {
        if(dark) {if(checked)Color(0xFF2D4B3B) else Color(0xFF243D30)} else {if(checked)Color(0xFFDEEEE2) else Color(0xFFEDF6EE)}
    } else if(checked)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(Modifier.fillMaxWidth().testTag("choice_$id").semantics { selected=checked }
        .toggleable(checked,role=Role.Checkbox,onValueChange={onClick()}),color=tint,shape=RoundedCornerShape(20.dp),
        border=BorderStroke(if(checked)2.dp else 1.dp,if(checked)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        if(centredLabel) {
            val density=LocalDensity.current
            val measure=rememberTextMeasurer()
            val available=(LocalConfiguration.current.screenWidthDp.dp-64.dp)/5-40.dp
            val labelSize=(22 downTo 16).firstOrNull{measure.measure(title,TextStyle(fontSize=it.sp,fontWeight=FontWeight.Bold),softWrap=false).size.width<=with(density){available.toPx()}}?:16
            val labelStyle=MaterialTheme.typography.titleLarge.copy(fontSize=labelSize.sp,fontWeight=FontWeight.Bold)
            val labelWidth=with(LocalDensity.current) { rememberTextMeasurer().measure(title,labelStyle,softWrap=false).size.width.toDp() }+1.dp
            Row(Modifier.heightIn(min=minHeight).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Text(title,style=labelStyle,maxLines=1,softWrap=false,modifier=Modifier.width(labelWidth).testTag("important_label_$id"))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(24.dp).testTag("selection_tick_$id").border(if(checked)2.dp else 1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(6.dp)),contentAlignment=Alignment.Center) {
                    if(checked)Icon(Icons.Default.Check,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))
                }
            }
        } else Row(Modifier.heightIn(min=if(veryShort)maxOf(48.dp,minHeight) else minHeight).padding(start=if(LocalDenseSelection.current)10.dp else if(short)8.dp else if(insetText)20.dp else if(compact)10.dp else 14.dp,end=if(LocalDenseSelection.current)10.dp else if(short)8.dp else if(insetText)12.dp else if(compact)10.dp else 14.dp,top=if(LocalDenseSelection.current)2.dp else if(veryShort)2.dp else if(dense)4.dp else if(compact)6.dp else 8.dp,bottom=if(LocalDenseSelection.current)2.dp else if(veryShort)2.dp else if(dense)4.dp else if(compact)6.dp else 8.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(title,style=if(short)MaterialTheme.typography.titleMedium.copy(fontSize=14.sp,lineHeight=18.sp) else MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                secondary?.invoke()
            }
            Box(Modifier.size(if(short)24.dp else 30.dp).testTag("selection_tick_$id").border(if(checked)2.dp else 1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center) {
                if(checked)Icon(Icons.Default.Check,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(if(short)22.dp else 28.dp))
            }
        }
    }
}
