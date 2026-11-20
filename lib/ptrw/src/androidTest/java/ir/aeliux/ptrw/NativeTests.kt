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
        val ptr: Long = dummy.requireHandle()
        pop(ptr)
        Assert.assertThrows(IllegalStateException::class.java) {
            dummy.requireHandle()
        }
        Assert.assertThrows(IllegalStateException::class.java) {
            pop(ptr)
        }
        Assert.assertTrue(dummy.isClosed())
        dummy.close()
    }

    @Test
    fun threadTest() {
        var dummy: DummyPointerWrapper? = null
        var ptr: Long = 0
        val thr = Thread() {
            dummy = DummyPointerWrapper.create()
            ptr = dummy.requireHandle()
        }
        thr.start()
        thr.join()
        Assert.assertThrows(IllegalStateException::class.java) {
            dummy?.requireHandle()
        }
        Assert.assertThrows(IllegalStateException::class.java) {
            pop(ptr)
        }
        // dummy memory will be leaked
    }

    companion object {
        init {
            System.loadLibrary("ptrwtests")
        }

        @JvmStatic
        private external fun pop(ptr: Long)
    }
}
