package com.jsonwizard.utils.zing;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.test.mock.MockContext;
import android.view.Display;
import android.view.WindowManager;

import com.jsonwizard.BaseTest;
import com.vijay.jsonwizard.utils.ImageUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class ImageUtilsTest extends BaseTest {
    @Mock
    private Context context;

    @Mock
    private WindowManager windowManager;

    @Mock
    private Display display;

    @Mock
    private BitmapFactory.Options options;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetDeviceWidth() {
        MockContext mockContext = Mockito.spy(MockContext.class);
        Mockito.doReturn(context).when(mockContext).getApplicationContext();
        Mockito.doReturn(windowManager).when(context).getSystemService(Context.WINDOW_SERVICE);
        Mockito.doReturn(display).when(windowManager).getDefaultDisplay();
        Assert.assertNotNull(context);

        int width = ImageUtils.getDeviceWidth(context);
        Assert.assertEquals(0,width);
    }

    @Test
    public void testCalculateInSampleSize(){
        options = new BitmapFactory.Options();
        Assert.assertNotNull(options);

        Whitebox.setInternalState(options, "outHeight", 50);
        Whitebox.setInternalState(options, "outWidth", 24);

        int inSampleSize = ImageUtils.calculateInSampleSize(options,3,5);
        Assert.assertEquals(4,inSampleSize);
    }

    @Test
    public void testCalculateInSampleSizeWithSmallerHeight(){
        options = new BitmapFactory.Options();
        Assert.assertNotNull(options);

        Whitebox.setInternalState(options, "outHeight", 1);
        Whitebox.setInternalState(options, "outWidth", 1);

        int inSampleSize = ImageUtils.calculateInSampleSize(options,3,5);
        Assert.assertEquals(1,inSampleSize);
    }

    @Test
    public void testCalculateInSampleSizeWithSmallHalfDimension(){
        options = new BitmapFactory.Options();
        Assert.assertNotNull(options);

        Whitebox.setInternalState(options, "outHeight", 4);
        Whitebox.setInternalState(options, "outWidth", 4);

        int inSampleSize = ImageUtils.calculateInSampleSize(options,3,5);
        Assert.assertEquals(1,inSampleSize);
    }
}