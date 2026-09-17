/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partitions: early/mid/late timeline, oversized/translated/thin models, absent/invalid geometry. */
class PresentationTest {
    @Test void delayedViewersReachTheSamePoseAndFinishedActionsReleaseIt() {
        assertEquals(0,GearMotion.reach(-1)); assertEquals(0,GearMotion.reach(0));
        assertEquals(1,GearMotion.reach(.5)); assertEquals(0,GearMotion.reach(1)); assertEquals(0,GearMotion.reach(10));
        assertEquals(GearMotion.reach(.2),GearMotion.reach(.8),1e-10);
        assertEquals(1,GearMotion.ease(100)); assertEquals(0,GearMotion.ease(-100));
    }
    @Test void oversizedAndOffsetModelsFitTheSameBodyBudget() {
        for (double size:new double[]{.01,1,16,200}) {
            var b=new ModelBounds(100,-30,9,100+size,-30+size*3,9+size/10);
            double fitted=b.fit(.92)*Math.sqrt(b.width()*b.width()+b.height()*b.height()+b.depth()*b.depth());
            assertEquals(.92,fitted,1e-9);
        }
    }
    @Test void invalidCustomModelsCannotProduceInfiniteScale() {
        assertThrows(IllegalArgumentException.class,()->new ModelBounds(0,0,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModelBounds(Double.NaN,0,0,1,1,1));
        assertThrows(IllegalArgumentException.class,()->new ModelBounds(1,0,0,0,1,1));
    }
    @Test void immediateCloseReversesTheCurrentOpeningInsteadOfJumpingToTheEnd() {
        assertEquals(GearMotion.ease(.25),GearMotion.closing(6,0),1e-10);
        assertTrue(GearMotion.closing(6,6)<GearMotion.closing(6,0));
        assertEquals(0,GearMotion.closing(6,24));
        assertEquals(1,GearMotion.closing(100,0));
    }
}
