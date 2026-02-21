package de.mstrauss.galactica.patterns

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class StateMachineTest {
    private val logger = object : Logger {
        override fun d(tag: String, message: String) = Unit
    }

    private lateinit var menuUi : MenuUi
    private lateinit var sm: MenuStateMachine
    @Mock lateinit var testState1: State
    @Mock lateinit var testState2: State
    @Mock lateinit var mockedActivity: AppCompatActivity
    @Mock lateinit var mockedView: View

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockedActivity.findViewById<View>(ArgumentMatchers.anyInt()))
            .thenReturn(mockedView)
        menuUi = MenuUi.bind(mockedActivity)
        sm = MenuStateMachine(menuUi, logger)
    }

    @Test
    fun stateMachine_noStateAtBeginning() {
        assertEquals("State is not null at beginning!", null, sm.state)
    }

    @Test
    fun stateMachine_correctStateInstanceChange() {
        sm.changeState(testState1)

        assertEquals("Enter State was not changed!", testState1, sm.state)
    }

    @Test
    fun stateMachine_enterStateWillBeExecuted() {
        sm.changeState(testState1)

        Mockito.verify(testState1, Mockito.times(1)).enterState(menuUi)
        Mockito.verify(testState1, Mockito.never()).exitState(menuUi)
    }

    @Test
    fun stateMachine_correctStateChangeMethodInvocation() {
        sm.changeState(testState1)

        Mockito.verify(testState1, Mockito.times(1)).enterState(menuUi)
        Mockito.verify(testState1, Mockito.never()).exitState(menuUi)
        Mockito.verify(testState2, Mockito.never()).enterState(menuUi)
        Mockito.verify(testState2, Mockito.never()).exitState(menuUi)

        sm.changeState(testState2)

        Mockito.verify(testState1).exitState(menuUi)
        Mockito.verify(testState1).enterState(menuUi)
    }
}
