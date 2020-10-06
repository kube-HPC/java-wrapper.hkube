package hkube.caching;

import hkube.model.HeaderContentPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public abstract class Cache<T> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    static Integer sizeLimit = null;
    static Integer accumulatingSize = 0;
    static Map<String, CacheItem> db = new HashMap();
    public static void init(Integer cacheLimit){
        sizeLimit = cacheLimit * 1000 * 1000;
        db = new HashMap();
        accumulatingSize = 0;
    }


    interface CacheItem {

        public Object getValue();

        public String getKey();

        public Long getTime();

        public Integer getSize();

    }


    public T get(String key) {
        CacheItem item = db.get(key);
        if (item != null) {
            return (T) item.getValue();
        } else {
            return null;
        }
    }

    public String put(String key, T value, Integer size) {
        if (size > sizeLimit) {
            logger.warn("Trying to insert a value of size " + size + " larger than " + sizeLimit);
            return null;
        }
        while ((size + accumulatingSize) > sizeLimit) {
            removeOldest();
        }
        CacheItem item = createItem(key, value,size);
        db.put(key, item);
        accumulatingSize += size;
        return key;
    }

    abstract CacheItem createItem(String key, T value, Integer size);

    private void removeOldest() {
        Iterator<CacheItem> iterator = db.values().iterator();
        CacheItem oldestItem = null;
        while (iterator.hasNext()) {
            CacheItem next = iterator.next();
            ;
            if (oldestItem == null) {
                oldestItem = next;
            }
            if (oldestItem.getTime() > next.getTime()) {
                oldestItem = next;
            }
        }
        db.remove(oldestItem.getKey());
        accumulatingSize -= oldestItem.getSize();
    }

    public Integer getNumberOfItems() {
        return db.size();
    }
}


