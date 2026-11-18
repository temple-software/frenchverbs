package com.templesoftware.frenchverbs.domain.use
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random
class UseCapacity38Test {
 @Test fun boundariesAndSavedCapacity() {
  for((length,longest,upper) in listOf(Triple(5,15,10),Triple(9,12,10),Triple(10,15,10),Triple(11,15,15),Triple(15,15,15))) {
   val seen=(0..1000).map{useBoxCapacity(length,longest,Random(it))}.toSet()
   assertEquals((length..upper).toSet(),seen)
  }
  val q=UseSentences.candidates(com.templesoftware.frenchverbs.domain.verbs.LearningItem("pouvoir",1,0),0,false,false).first().copy(chosenBoxCount=10)
  val restored=UseSessions.decode(q.encode());assertEquals(q,restored);assertEquals(q.boxCount,restored.boxCount)
  var state=UseActions.start(List(10){restored});val capacity=restored.boxCount
  repeat(3){state=UseActions.letter(state,restored.keys.first());state=UseActions.clear(state);assertEquals(capacity,state.current!!.boxCount)}
 }
}
