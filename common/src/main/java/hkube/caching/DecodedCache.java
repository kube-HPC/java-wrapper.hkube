package hkube.caching;

import hkube.model.HeaderContentPair;
import hkube.model.ObjectAndSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DecodedCache extends Cache<Object>{

    public DecodedCache() {
    }

    Map<String, CacheItem> db = new HashMap();

    private class CacheItem implements Cache.CacheItem{
        Object value;
        String key;
        long time;
        Integer size;

        public String getKey() {
            return key;
        }

        public Long getTime() {
            return time;
        }

        public Integer getSize() {
            return size;
        }

        public CacheItem(Object value, String key, Long time, Integer size) {
            this.value = value;
            this.key = key;
            this.time = time;
            this.size =size;
        }

        public Object getValue() {
            return value;
        }
    }
    @Override
    Cache.CacheItem createItem(String key, Object value,Integer size) {
        return new CacheItem(value,key,new Date().getTime(),size);
    }

}
