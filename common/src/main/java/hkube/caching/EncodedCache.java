package hkube.caching;

import hkube.model.HeaderContentPair;
import java.util.Date;


public class EncodedCache extends Cache<HeaderContentPair> {
    Integer sizeLimit;
    public EncodedCache() {
    }
    private class CacheItem implements Cache.CacheItem {
        HeaderContentPair value;
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

        public CacheItem(HeaderContentPair value, String key, Long time ,Integer size) {
            this.value = value;
            this.key = key;
            this.time = time;
            this.size = size;
        }

        public HeaderContentPair getValue() {
            return value;
        }
    }

    @Override
    Cache.CacheItem createItem(String key, HeaderContentPair value,Integer size) {
        return new CacheItem(value,key,new Date().getTime(),size);
    }

}
