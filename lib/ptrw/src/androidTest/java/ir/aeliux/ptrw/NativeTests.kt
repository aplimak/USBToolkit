package ir.aeliux.ptrw

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeTests {
    @Test
    fun popTest() {
        val dummy: DummyPointerWrapper = DummyPointerWrapper.create()
        Assert.assertTrue(dummy.requireHandle() != 0.toLong())
        Assert.assertFalse(dummy.isClosed())
        pop(dummy.requireHandle())
        Assert.assertThrows(IllegalStateException::class.java) {
            dummy.requireHandle()
        }
        Assert.assertTrue(dummy.isClosed())
        dummy.close()
    }

    companion object {
        init {
            System.loadLibrary("ptrw")
        }

        @JvmStatic
        private external fun pop(ptr: Long)
    }
}
