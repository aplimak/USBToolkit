package ir.aeliux.ptrw;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import ir.aeliux.gtestrunner.GTestLibrary;
import ir.aeliux.gtestrunner.GTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.DuplicateFormatFlagsException;

@RunWith(GTestRunner.class)
@GTestLibrary("ptrwtests")
public class GTests {
    static public Runnable testCallback = () -> {
        throw new DuplicateFormatFlagsException("ptrw_callback");
    };
}
