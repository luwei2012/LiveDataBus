package com.jeremyliao.livedatabus.liveevent;

import android.support.annotation.Nullable;


/**
 * 项目名称：组件化产品
 * 类名称：com.jeremyliao.livedatabus.liveevent.Observer.java
 * 描述: <描述当前版本功能>
 *
 * @author luwei
 * @version [版本号, 2018/12/2]
 */
public interface LiveEventObserver<T> {
    /**
     * Called when the data is changed.
     *
     * @param t The new data
     * @return is the data  consumed
     */
    boolean onChanged(@Nullable T t);
}
