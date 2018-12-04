# LiveEventBus

### Android消息总线，Thanks to [JeremyLiao](https://github.com/JeremyLiao/LiveDataBus/blob/master/live-data-bus/livedatabus/src/main/java/com/jeremyliao/livedatabus/LiveDataBus.java)

### 与LiveDataBus的不同
1.废弃了LiveDataBus，完全抛弃了LiveData的相关依赖，只保留了对LifeCycle的核心依赖
2.给观察者添加了权重，值越低优先级越高，0表示最高优先级。默认给每个观察者的权重为16，0~15预留给系统级观察者。用一优先级的观察者采用FIFO的原则，先注册，先通知。
3.事件可被消费，被消费后其他观察者无法收到通知。你可以在消费后再次恢复事件，继续通知其他未收到事件的观察者。
4.针对同一类型的事件，在3的情况下，新事件会覆盖旧事件，导致旧事件无法re-dispatching。

### 主要功能Commit记录
1. 在LiveDataBus的基础上扩展功能2和3

## 如何使用本项目
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency
```
	dependencies {
		implementation 'com.github.luwei2012:LiveEventBus:1.0.0'
	}
```

## 调用方式

#### 订阅消息
- **observe**
生命周期感知，不需要手动取消订阅

```java
LiveEventBus.get()
	.with("key_name", String.class)
	.observe(this, new Observer<String>() {
	    @Override
	    public void onChanged(@Nullable String s) {
	       
	    }
	});
```
- **observeForever**
需要手动取消订阅

```java
LiveEventBus.get()
	.with("key_name", String.class)
	.observeForever(observer);
```

```java
LiveEventBus.get()
	.with("key_name", String.class)
	.removeObserver(observer);
```

#### 发送消息
- **setValue**
在主线程发送消息
```java
LiveEventBus.get().with("key_name").setValue(value);
```
- **postValue**
在后台线程发送消息，订阅者会在主线程收到消息
```java
LiveEventBus.get().with("key_name").postValue(value);
```
#### Sticky模式
支持在注册订阅者的时候设置Sticky模式，这样订阅者可以接收到订阅之前发送的消息

- **observeSticky**
生命周期感知，不需要手动取消订阅，Sticky模式

```java
LiveEventBus.get()
        .with("sticky_key", String.class)
        .observeSticky(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
             
            }
        });
```
- **observeStickyForever**
需要手动取消订阅，Sticky模式

```java
LiveEventBus.get()
        .with("sticky_key", String.class)
        .observeStickyForever(observer);
```

```java
LiveEventBus.get()
        .with("sticky_key", String.class)
        .removeObserver(observer);
```

- **resumeDispatch**
只有在事件被消费后、新的事件产生前调用有用，会继续通知其他观察者

```java
LiveEventBus.get()
        .with("key_name", String.class)
        .resumeDispatch("value");
```

## 示例和DEMO

##### 事件消费+re-dispatching
![事件消费](https://github.com/luwei2012/LiveEventBus/blob/master/images/img1.gif) 






