package com.templesoftware.frenchverbs.domain.verbs

fun Selection.readyForActivities():Boolean =
    setupRoutes().filter{it!="activities"}.all{canContinue(it)}

data class WelcomeCategory(val category:String,val section:String?,val selected:List<String>,val total:Int) {
    val showNames get()=total<=3 || selected.size<=3
}
fun welcomeCategories(selection:Selection):List<WelcomeCategory> = Choices.types.filter{it in selection.types}.map { category ->
    val section=when(category){"important"->"important";"er"->"er";"irregular"->"families";else->null}
    val options=section?.let{selection.visible(it)}.orEmpty()
    WelcomeCategory(category,section,options.filter{it in selection.selected(section.orEmpty())},options.size)
}
