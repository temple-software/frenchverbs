package com.templesoftware.frenchverbs.ui.use
import org.junit.Assert.*
import org.junit.Test
class UseEntryDisplayTest {
    @Test fun lettersSpacesApostrophesAndTruncation() {
        assertEquals("",useDisplayEntry("avait terminé",""))
        assertEquals("avait",useDisplayEntry("avait terminé","avait"))
        assertEquals("avait t",useDisplayEntry("avait terminé","avaitt"))
        assertEquals("avait terminé",useDisplayEntry("avait terminé","avaitterminé"))
        assertEquals("avai",useDisplayEntry("avait terminé","avai"))
        assertEquals("a’b",useDisplayEntry("a’b","ab"))
        assertEquals("protégeons",useDisplayEntry("protégeons","protégeons"))
        assertEquals("prot",useDisplayEntry("protégeons","prot"))
        assertEquals("avait xyz",useDisplayEntry("avait terminé","avaitxyz"))
    }
}
