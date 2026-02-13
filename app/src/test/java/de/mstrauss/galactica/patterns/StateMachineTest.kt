package de.mstrauss.galactica.patterns

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class StateMachineTest {
    private val logger = object : Logger {
        override fun d(tag: String, message: String) = Unit
    }

    private val sm = MenuStateMachine(logger)

    @Mock lateinit var testState1: State
    @Mock lateinit var testState2: State
    @Mock lateinit var mockedActivity: AppCompatActivity

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun stateMachine_noStateAtBeginning() {
        assertEquals("State is not null at beginning!", null, sm.state)
    }

    @Test
    fun stateMachine_correctStateInstanceChange() {
        sm.changeState(testState1, mockedActivity)

        assertEquals("Enter State was not changed!", testState1, sm.state)
    }

    @Test
    fun stateMachine_enterStateWillBeExecuted() {
        sm.changeState(testState1, mockedActivity)

        Mockito.verify(testState1, Mockito.times(1)).enterState(mockedActivity)
        Mockito.verify(testState1, Mockito.never()).exitState(mockedActivity)
    }

    @Test
    fun stateMachine_correctStateChangeMethodInvocation() {
        sm.changeState(testState1, mockedActivity)

        Mockito.verify(testState1, Mockito.times(1)).enterState(mockedActivity)
        Mockito.verify(testState1, Mockito.never()).exitState(mockedActivity)
        Mockito.verify(testState2, Mockito.never()).enterState(mockedActivity)
        Mockito.verify(testState2, Mockito.never()).exitState(mockedActivity)

        sm.changeState(testState2, mockedActivity)

        Mockito.verify(testState1).exitState(mockedActivity)
        Mockito.verify(testState1).enterState(mockedActivity)
    }
}
