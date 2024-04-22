package io.github.orangewest.trans.resolver;

import java.util.ArrayList;
import java.util.List;

public class TransObjResolverFactory {

    private final static List<TransObjResolver> RESOLVERS = new ArrayList<>();

    public static void register(TransObjResolver resolver) {
        RESOLVERS.add(resolver);
    }

    public static List<TransObjResolver> getResolvers() {
        return RESOLVERS;
    }

}
