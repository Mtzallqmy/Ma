package com.dawahayah.app

import com.dawahayah.app.domain.model.EducationalCalculators as Calc
import org.junit.Assert.*
import org.junit.Test

class EducationalCalculatorsTest {
    private fun reject(action: () -> Unit) { try { action(); fail("Expected invalid model input") } catch (_: IllegalArgumentException) {} }
    @Test fun unitConversionsPreserveDimensionsAndRoundTrip() {
        assertEquals(250.0,Calc.convert(0.25,Calc.Unit.G,Calc.Unit.MG),1e-9)
        assertEquals(0.25,Calc.convert(250.0,Calc.Unit.MG,Calc.Unit.G),1e-12)
        assertEquals(2000.0,Calc.convert(2.0,Calc.Unit.L,Calc.Unit.ML),1e-9)
        assertEquals(0.0,Calc.convert(0.0,Calc.Unit.G,Calc.Unit.MG),0.0)
        reject { Calc.convert(1.0,Calc.Unit.G,Calc.Unit.ML) }
    }
    @Test fun dilutionConservesSoluteAndCannotConcentrate() {
        val v=Calc.dilutionStockVolume(10.0,2.0,50.0)
        assertEquals(10.0,v,1e-12);assertEquals(2.0*50.0,10.0*v,1e-12)
        assertEquals(50.0,Calc.dilutionStockVolume(2.0,2.0,50.0),1e-12)
        reject { Calc.dilutionStockVolume(2.0,10.0,50.0) }
        reject { Calc.dilutionStockVolume(0.0,2.0,50.0) }
        reject { Calc.dilutionStockVolume(10.0,0.0,50.0) }
        reject { Calc.dilutionStockVolume(10.0,2.0,0.0) }
    }
    @Test fun halfLifeModelHasCorrectBoundariesAndNoPatientDose() {
        assertEquals(100.0,Calc.remainingPercent(0.0,4.0),1e-12)
        assertEquals(50.0,Calc.remainingPercent(4.0,4.0),1e-12)
        assertEquals(12.5,Calc.remainingPercent(12.0,4.0),1e-12)
        assertEquals(100.0/kotlin.math.sqrt(2.0),Calc.remainingPercent(2.0,4.0),1e-10)
        reject { Calc.remainingPercent(1.0,0.0) };reject { Calc.remainingPercent(-1.0,4.0) }
        reject { Calc.remainingPercent(101.0,1.0) }
    }
    @Test fun invalidNumbersAndOverflowAreRejected() {
        for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1.0,1e13)) {
            reject { Calc.convert(value,Calc.Unit.G,Calc.Unit.MG) }
        }
        reject { Calc.convert(1e12,Calc.Unit.G,Calc.Unit.MCG) }
    }
}
