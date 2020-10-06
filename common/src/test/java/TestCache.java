import hkube.caching.Cache;
import hkube.caching.EncodedCache;
import hkube.model.HeaderContentPair;
import org.junit.Test;
import org.junit.Assert;

public class TestCache {
    @Test
    public void testReachingLimit() {
        Cache.init(4);
        EncodedCache encodedCache = new EncodedCache();
        HeaderContentPair pair = new HeaderContentPair(null, new byte[1000000]);
        encodedCache.put("task1", pair, 1000000);
        Assert.assertEquals(encodedCache.getNumberOfItems(), Integer.valueOf(1));
        pair = new HeaderContentPair(null, new byte[1000000]);
        encodedCache.put("task2", pair, 1000000);
        pair = new HeaderContentPair(null, new byte[1000000]);
        encodedCache.put("task3", pair, 1000000);
        Assert.assertEquals( encodedCache.getNumberOfItems(), Integer.valueOf(3));
        pair = new HeaderContentPair(null, new byte[1000001]);
        encodedCache.put("task4", pair, 1000001);
        Assert.assertEquals(encodedCache.getNumberOfItems(), Integer.valueOf(3));
        Assert.assertNotNull(encodedCache.get("task4"));
        Assert.assertNull(encodedCache.get("task1"));
    }

    @Test
    public void testTooLarge() {
        Cache.init(4);
        EncodedCache encodedCache = new EncodedCache();
        HeaderContentPair pair = new HeaderContentPair(null, new byte[1000000]);
        encodedCache.put("task1", pair,1000000);
        Assert.assertEquals(encodedCache.getNumberOfItems(), Integer.valueOf(1));
        pair = new HeaderContentPair(null, new byte[4000001]);
        encodedCache.put("task2", pair,4000001);
        Assert.assertEquals(encodedCache.getNumberOfItems(), Integer.valueOf(1));
        Assert.assertNotNull(encodedCache.get("task1"));
        Assert.assertNull(encodedCache.get("task2"));
    }
}
