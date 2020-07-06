import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public class PassBytes implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map args, IHKubeAPI hkubeAPI) throws Exception {
        ByteBuffer buffer = (ByteBuffer) ((Collection) args.get("input")).iterator().next();
        byte[] byteArr = new byte[buffer.remaining()];
        buffer.get(byteArr);
        byteArr[6] = 6;
        return byteArr;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
