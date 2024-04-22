package io.github.orangewest.trans.util;


import io.github.orangewest.trans.service.TransService;

public class TransUtil {

    /**
     * 翻译工具
     *
     * @param obj 需要翻译的对象
     * @return 是否翻译成功
     */
    public static boolean trans(Object obj) {
        return TransServiceHolder.get().trans(obj);
    }

    static class TransServiceHolder {
        private static final TransService INSTANCE = new TransService();

        public static TransService get() {
            return INSTANCE;
        }
    }

}
