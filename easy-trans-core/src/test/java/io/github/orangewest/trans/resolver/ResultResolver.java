package io.github.orangewest.trans.resolver;

import io.github.orangewest.trans.dto.Result;

public class ResultResolver implements TransObjResolver {
    @Override
    public boolean support(Object obj) {
        return obj instanceof Result;
    }

    @Override
    public Object resolveTransObj(Object obj) {
        return ((Result<?>) obj).getData();
    }

}
